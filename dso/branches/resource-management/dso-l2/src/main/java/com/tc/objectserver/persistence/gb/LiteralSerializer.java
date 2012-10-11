package com.tc.objectserver.persistence.gb;

import org.terracotta.corestorage.Serializer;

import com.tc.object.ObjectID;
import com.tc.object.dna.impl.UTF8ByteDataHolder;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tim
 */
public class LiteralSerializer implements Serializer<Object> {
  public static final LiteralSerializer INSTANCE = new LiteralSerializer();

  private static enum Type {
    LONG {
      @Override
      public Long deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.getLong();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Long) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + Long.SIZE / Byte.SIZE);
          buffer.put((byte) ordinal()).putLong((Long)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Long.class;
      }
    }, INT {
      @Override
      public Integer deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.getInt();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Integer) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + Integer.SIZE / Byte.SIZE);
          buffer.put((byte) ordinal()).putInt((Integer)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Integer.class;
      }
    }, SHORT {
      @Override
      public Short deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.getShort();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Short) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + Short.SIZE / Byte.SIZE);
          buffer.put((byte) ordinal()).putShort((Short)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Short.class;
      }
    }, BYTE {
      @Override
      public Byte deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.get();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Short) {
          ByteBuffer buffer = ByteBuffer.allocate(2);
          buffer.put((byte) ordinal()).put((Byte)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Byte.class;
      }
    }, STRING {
      @Override
      public String deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.asCharBuffer().toString();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof String) {
          String s = (String) object;
          ByteBuffer buffer = ByteBuffer.allocate(1 + s.length() * 2);
          buffer.put((byte)ordinal());
          buffer.position(buffer.position() + buffer.asCharBuffer().put(s).position() * 2);
          buffer.flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return String.class;
      }
    }, OBJECTID {
      @Override
      public ObjectID deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return new ObjectID(buffer.getLong());
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof ObjectID) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + Long.SIZE / Byte.SIZE);
          buffer.put((byte)ordinal()).putLong(((ObjectID)object).toLong()).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return ObjectID.class;
      }
    }, UTF8BYTES {
      @Override
      Object deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return new UTF8ByteDataHolder(bytes);
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof UTF8ByteDataHolder) {
          byte[] bytes = ((UTF8ByteDataHolder)object).getBytes();
          ByteBuffer buffer = ByteBuffer.allocate(1 + Integer.SIZE / Byte.SIZE + bytes.length);
          buffer.put((byte)ordinal()).putInt(bytes.length).put(bytes).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return UTF8ByteDataHolder.class;
      }
    };

    abstract Object deserialize(ByteBuffer buffer);

    abstract ByteBuffer serialize(Object object);

    abstract Class<?> toClass();
  }

  private static final Map<Class<?>, Type> classToType;
  static {
    classToType = new HashMap<Class<?>, Type>();
    for (Type type : Type.values()) {
      classToType.put(type.toClass(), type);
    }
  }


  @Override
  public Object deserialize(final ByteBuffer buffer) {
    return Type.values()[buffer.duplicate().get()].deserialize(buffer);
  }

  @Override
  public ByteBuffer serialize(final Object o) {
    if (o == null) {
      throw new IllegalArgumentException("Serializing a null is not supported.");
    }
    if (!classToType.containsKey(o.getClass())) {
      throw new IllegalArgumentException("Unknown type " + o + " class " + o.getClass());
    }
    return classToType.get(o.getClass()).serialize(o);
  }

  @Override
  public boolean equals(final ByteBuffer left, final Object right) {
    return deserialize(left).equals(right);
  }
}
