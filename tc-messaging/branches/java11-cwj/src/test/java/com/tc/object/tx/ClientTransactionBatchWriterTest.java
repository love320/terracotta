package com.tc.object.tx;

import com.tc.net.GroupID;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.util.SequenceID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Clifford W. Johnson
 */
public class ClientTransactionBatchWriterTest {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    @Mock
    private GroupID groupID;
    @Mock
    private TxnBatchID batchID;
    @Mock
    private ObjectStringSerializer serializer;
    @Mock
    private DNAEncodingInternal encoding;
    @Mock
    private CommitTransactionMessageFactory transactionMessageFactory;
    @Mock
    private ClientTransactionBatchWriter.FoldingConfig foldingConfig;

    @Mock
    private ClientTransaction clientTransaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Ensures {@link ClientTransactionBatchWriter#getTransactionBatchID()} waits until
     * data is written to the batch.
     *
     * @since TAB-6584
     */
    @Test
    public void testGetTransactionBatchID() throws Exception {
        when(this.clientTransaction.getLockType()).thenReturn(TxnType.NORMAL);
        when(this.clientTransaction.getTransactionCompleteListeners()).thenReturn(Collections.emptyList());
        when(this.clientTransaction.getReferencesOfObjectsInTxn()).thenReturn(Collections.emptyList());
        final SequenceID sequenceID = new SequenceID(0);
        when(this.clientTransaction.getSequenceID()).thenReturn(sequenceID);
        final TransactionID transactionID = new TransactionID(1);
        when(this.clientTransaction.getTransactionID()).thenReturn(transactionID);

        final ClientTransactionBatchWriter writer =
                new ClientTransactionBatchWriter(this.groupID, this.batchID, this.serializer, this.encoding,
                        this.transactionMessageFactory, this.foldingConfig);

        final TransactionBuffer transactionBuffer = writer.addSimpleTransaction(this.clientTransaction);
        assertThat(transactionBuffer, is(not(nullValue())));

        /*
         * In another thread, attempt to get the TransactionBatchID -- this call should wait until
         * write() is called releasing the buffer.
         */
        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final AtomicReference<TxnBatchID> transactionBatchID = new AtomicReference<TxnBatchID>();
        final Thread accessor = new Thread(new Runnable() {
            @Override
            public void run() {
                transactionBatchID.set(writer.getTransactionBatchID());
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        }, "testGetTransactionBatchID Thread");
        accessor.setDaemon(true);
        accessor.start();

        /*
         * Wait until the accessor thread is waiting on the ClientTransactionBatchWriter monitor
         * (presumably in getTransactionBatchID).
         */
        Thread.State threadState;
        threadState = waitForState(accessor, EnumSet.of(Thread.State.TERMINATED, Thread.State.WAITING, Thread.State.TIMED_WAITING));
        // ClientTransactionBatchWriter.getTransactionBatchID presently uses wait(); a change to wait(long) will fail here
        assertThat(threadState, is(Thread.State.WAITING));
        assertThat(THREAD_MX_BEAN.getThreadInfo(accessor.getId()).getLockInfo().getClassName(),
                is(equalTo(ClientTransactionBatchWriter.class.getName())));

        /*
         * At this point, we know the accessor thread is waiting on the ClientTransactionBatchWriter
         * monitor -- presumably in ClientTransactionBatchWriter.getTransactionID.  Interrupt the
         * accessor thread to ensure it remains waiting on the ClientTransactionBatchWriter monitor.
         * The method (and the thread) should not terminate due to the interruption -- the method
         * should resume waiting.
         */
        accessor.interrupt();
        threadState = waitForState(accessor, EnumSet.of(Thread.State.TERMINATED, Thread.State.WAITING));
        assertThat(threadState, is(Thread.State.WAITING));
        assertThat(THREAD_MX_BEAN.getThreadInfo(accessor.getId()).getLockInfo().getClassName(),
                is(equalTo(ClientTransactionBatchWriter.class.getName())));

        /*
         * Now complete the write to the transaction buffer to release the accessor thread.
         */
        transactionBuffer.write(this.clientTransaction);

        /*
         * Wait for the accessor thread to complete.  The getTransactionBatchID method is supposed
         * to re-assert the thread interrupt status -- ensure that is so.
         */
        accessor.join(500L);
        assertThat(interrupted.get(), is(true));
    }

    private Thread.State waitForState(Thread accessor, EnumSet<Thread.State> endingStates) throws InterruptedException {
        Thread.State threadState;
        do {
            Thread.sleep(50L);      // Permit target thread to take action
            threadState = accessor.getState();
        } while (!endingStates.contains(threadState));
        return threadState;
    }
}