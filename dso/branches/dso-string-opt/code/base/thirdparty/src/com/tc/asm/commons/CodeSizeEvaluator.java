/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2005 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tc.asm.commons;

import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

/**
 * A {@link MethodAdapter} that can be used to approximate method size.
 * 
 * @author Eugene Kuleshov
 */
public class CodeSizeEvaluator extends MethodAdapter implements Opcodes {

    private int minSize;

    private int maxSize;

    public CodeSizeEvaluator(final MethodVisitor mv) {
        super(mv);
    }

    public int getMinSize() {
        return this.minSize;
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public void visitInsn(final int opcode) {
        minSize += 1;
        maxSize += 1;
        if (mv != null) {
            mv.visitInsn(opcode);
        }
    }

    public void visitIntInsn(final int opcode, final int operand) {
        if (opcode == SIPUSH) {
            minSize += 3;
            maxSize += 3;
        } else {
            minSize += 2;
            maxSize += 2;
        }
        if (mv != null) {
            mv.visitIntInsn(opcode, operand);
        }
    }

    public void visitVarInsn(final int opcode, final int var) {
        if (var < 4 && opcode != Opcodes.RET) {
            minSize += 1;
            maxSize += 1;
        } else if (var >= 256) {
            minSize += 4;
            maxSize += 4;
        } else {
            minSize += 2;
            maxSize += 2;
        }
        if (mv != null) {
            mv.visitVarInsn(opcode, var);
        }
    }

    public void visitTypeInsn(final int opcode, final String desc) {
        minSize += 3;
        maxSize += 3;
        if (mv != null) {
            mv.visitTypeInsn(opcode, desc);
        }
    }

    public void visitFieldInsn(
        final int opcode,
        final String owner,
        final String name,
        final String desc)
    {
        minSize += 3;
        maxSize += 3;
        if (mv != null) {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    public void visitMethodInsn(
        final int opcode,
        final String owner,
        final String name,
        final String desc)
    {
        if (opcode == INVOKEINTERFACE) {
            minSize += 5;
            maxSize += 5;
        } else {
            minSize += 3;
            maxSize += 3;
        }
        if (mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc);
        }
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        minSize += 3;
        if (opcode == GOTO || opcode == JSR) {
            maxSize += 5;
        } else {
            maxSize += 8;
        }
        if (mv != null) {
            mv.visitJumpInsn(opcode, label);
        }
    }

    public void visitLdcInsn(final Object cst) {
        if (cst instanceof Long || cst instanceof Double) {
            minSize += 3;
            maxSize += 3;
        } else {
            minSize += 2;
            maxSize += 3;
        }
        if (mv != null) {
            mv.visitLdcInsn(cst);
        }
    }

    public void visitIincInsn(final int var, final int increment) {
        if (var > 255 || increment > 127 || increment < -128) {
            minSize += 6;
            maxSize += 6;
        } else {
            minSize += 3;
            maxSize += 3;
        }
        if (mv != null) {
            mv.visitIincInsn(var, increment);
        }
    }

    public void visitTableSwitchInsn(
        final int min,
        final int max,
        final Label dflt,
        final Label[] labels)
    {
        minSize += 13 + labels.length * 4;
        maxSize += 16 + labels.length * 4;
        if (mv != null) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    public void visitLookupSwitchInsn(
        final Label dflt,
        final int[] keys,
        final Label[] labels)
    {
        minSize += 9 + keys.length * 8;
        maxSize += 12 + keys.length * 8;
        if (mv != null) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        minSize += 4;
        maxSize += 4;
        if (mv != null) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
    }
}
