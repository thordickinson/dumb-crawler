/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.thordickinson.dumbcrawler.services.storage.avro.schema;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class WebPage extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 1084889630683318141L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"WebPage\",\"namespace\":\"com.thordickinson.dumbcrawler.services.storage.avro.schema\",\"fields\":[{\"name\":\"url\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"contentType\",\"type\":\"string\"},{\"name\":\"content\",\"type\":\"string\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<WebPage> ENCODER =
      new BinaryMessageEncoder<WebPage>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<WebPage> DECODER =
      new BinaryMessageDecoder<WebPage>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<WebPage> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<WebPage> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<WebPage> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<WebPage>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this WebPage to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a WebPage from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a WebPage instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static WebPage fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  private java.lang.CharSequence url;
  private long timestamp;
  private java.lang.CharSequence contentType;
  private java.lang.CharSequence content;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public WebPage() {}

  /**
   * All-args constructor.
   * @param url The new value for url
   * @param timestamp The new value for timestamp
   * @param contentType The new value for contentType
   * @param content The new value for content
   */
  public WebPage(java.lang.CharSequence url, java.lang.Long timestamp, java.lang.CharSequence contentType, java.lang.CharSequence content) {
    this.url = url;
    this.timestamp = timestamp;
    this.contentType = contentType;
    this.content = content;
  }

  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return url;
    case 1: return timestamp;
    case 2: return contentType;
    case 3: return content;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: url = (java.lang.CharSequence)value$; break;
    case 1: timestamp = (java.lang.Long)value$; break;
    case 2: contentType = (java.lang.CharSequence)value$; break;
    case 3: content = (java.lang.CharSequence)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'url' field.
   * @return The value of the 'url' field.
   */
  public java.lang.CharSequence getUrl() {
    return url;
  }


  /**
   * Sets the value of the 'url' field.
   * @param value the value to set.
   */
  public void setUrl(java.lang.CharSequence value) {
    this.url = value;
  }

  /**
   * Gets the value of the 'timestamp' field.
   * @return The value of the 'timestamp' field.
   */
  public long getTimestamp() {
    return timestamp;
  }


  /**
   * Sets the value of the 'timestamp' field.
   * @param value the value to set.
   */
  public void setTimestamp(long value) {
    this.timestamp = value;
  }

  /**
   * Gets the value of the 'contentType' field.
   * @return The value of the 'contentType' field.
   */
  public java.lang.CharSequence getContentType() {
    return contentType;
  }


  /**
   * Sets the value of the 'contentType' field.
   * @param value the value to set.
   */
  public void setContentType(java.lang.CharSequence value) {
    this.contentType = value;
  }

  /**
   * Gets the value of the 'content' field.
   * @return The value of the 'content' field.
   */
  public java.lang.CharSequence getContent() {
    return content;
  }


  /**
   * Sets the value of the 'content' field.
   * @param value the value to set.
   */
  public void setContent(java.lang.CharSequence value) {
    this.content = value;
  }

  /**
   * Creates a new WebPage RecordBuilder.
   * @return A new WebPage RecordBuilder
   */
  public static com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder newBuilder() {
    return new com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder();
  }

  /**
   * Creates a new WebPage RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new WebPage RecordBuilder
   */
  public static com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder newBuilder(com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder other) {
    if (other == null) {
      return new com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder();
    } else {
      return new com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder(other);
    }
  }

  /**
   * Creates a new WebPage RecordBuilder by copying an existing WebPage instance.
   * @param other The existing instance to copy.
   * @return A new WebPage RecordBuilder
   */
  public static com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder newBuilder(com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage other) {
    if (other == null) {
      return new com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder();
    } else {
      return new com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder(other);
    }
  }

  /**
   * RecordBuilder for WebPage instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<WebPage>
    implements org.apache.avro.data.RecordBuilder<WebPage> {

    private java.lang.CharSequence url;
    private long timestamp;
    private java.lang.CharSequence contentType;
    private java.lang.CharSequence content;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.url)) {
        this.url = data().deepCopy(fields()[0].schema(), other.url);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[1].schema(), other.timestamp);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.contentType)) {
        this.contentType = data().deepCopy(fields()[2].schema(), other.contentType);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.content)) {
        this.content = data().deepCopy(fields()[3].schema(), other.content);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
    }

    /**
     * Creates a Builder by copying an existing WebPage instance
     * @param other The existing instance to copy.
     */
    private Builder(com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.url)) {
        this.url = data().deepCopy(fields()[0].schema(), other.url);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[1].schema(), other.timestamp);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.contentType)) {
        this.contentType = data().deepCopy(fields()[2].schema(), other.contentType);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.content)) {
        this.content = data().deepCopy(fields()[3].schema(), other.content);
        fieldSetFlags()[3] = true;
      }
    }

    /**
      * Gets the value of the 'url' field.
      * @return The value.
      */
    public java.lang.CharSequence getUrl() {
      return url;
    }


    /**
      * Sets the value of the 'url' field.
      * @param value The value of 'url'.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder setUrl(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.url = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'url' field has been set.
      * @return True if the 'url' field has been set, false otherwise.
      */
    public boolean hasUrl() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'url' field.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder clearUrl() {
      url = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'timestamp' field.
      * @return The value.
      */
    public long getTimestamp() {
      return timestamp;
    }


    /**
      * Sets the value of the 'timestamp' field.
      * @param value The value of 'timestamp'.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder setTimestamp(long value) {
      validate(fields()[1], value);
      this.timestamp = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'timestamp' field has been set.
      * @return True if the 'timestamp' field has been set, false otherwise.
      */
    public boolean hasTimestamp() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'timestamp' field.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder clearTimestamp() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'contentType' field.
      * @return The value.
      */
    public java.lang.CharSequence getContentType() {
      return contentType;
    }


    /**
      * Sets the value of the 'contentType' field.
      * @param value The value of 'contentType'.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder setContentType(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.contentType = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'contentType' field has been set.
      * @return True if the 'contentType' field has been set, false otherwise.
      */
    public boolean hasContentType() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'contentType' field.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder clearContentType() {
      contentType = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'content' field.
      * @return The value.
      */
    public java.lang.CharSequence getContent() {
      return content;
    }


    /**
      * Sets the value of the 'content' field.
      * @param value The value of 'content'.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder setContent(java.lang.CharSequence value) {
      validate(fields()[3], value);
      this.content = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'content' field has been set.
      * @return True if the 'content' field has been set, false otherwise.
      */
    public boolean hasContent() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'content' field.
      * @return This builder.
      */
    public com.thordickinson.dumbcrawler.services.storage.avro.schema.WebPage.Builder clearContent() {
      content = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WebPage build() {
      try {
        WebPage record = new WebPage();
        record.url = fieldSetFlags()[0] ? this.url : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.timestamp = fieldSetFlags()[1] ? this.timestamp : (java.lang.Long) defaultValue(fields()[1]);
        record.contentType = fieldSetFlags()[2] ? this.contentType : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.content = fieldSetFlags()[3] ? this.content : (java.lang.CharSequence) defaultValue(fields()[3]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<WebPage>
    WRITER$ = (org.apache.avro.io.DatumWriter<WebPage>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<WebPage>
    READER$ = (org.apache.avro.io.DatumReader<WebPage>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeString(this.url);

    out.writeLong(this.timestamp);

    out.writeString(this.contentType);

    out.writeString(this.content);

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.url = in.readString(this.url instanceof Utf8 ? (Utf8)this.url : null);

      this.timestamp = in.readLong();

      this.contentType = in.readString(this.contentType instanceof Utf8 ? (Utf8)this.contentType : null);

      this.content = in.readString(this.content instanceof Utf8 ? (Utf8)this.content : null);

    } else {
      for (int i = 0; i < 4; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.url = in.readString(this.url instanceof Utf8 ? (Utf8)this.url : null);
          break;

        case 1:
          this.timestamp = in.readLong();
          break;

        case 2:
          this.contentType = in.readString(this.contentType instanceof Utf8 ? (Utf8)this.contentType : null);
          break;

        case 3:
          this.content = in.readString(this.content instanceof Utf8 ? (Utf8)this.content : null);
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










