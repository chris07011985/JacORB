package org.jacorb.test.orb.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import org.jacorb.config.Configuration;
import org.jacorb.config.JacORBConfiguration;
import org.jacorb.orb.giop.ClientConnection;
import org.jacorb.orb.giop.GIOPConnection;
import org.jacorb.orb.giop.GIOPConnectionManager;
import org.jacorb.orb.giop.LocateRequestOutputStream;
import org.jacorb.orb.giop.MessageOutputStream;
import org.jacorb.orb.giop.Messages;
import org.jacorb.orb.giop.ReplyInputStream;
import org.jacorb.orb.giop.ReplyListener;
import org.jacorb.orb.giop.RequestInputStream;
import org.jacorb.orb.giop.RequestListener;
import org.jacorb.orb.giop.RequestOutputStream;
import org.jacorb.orb.giop.ServerGIOPConnection;
import org.jacorb.orb.iiop.IIOPAddress;
import org.jacorb.orb.iiop.IIOPProfile;
import org.jacorb.test.common.ORBTestCase;
import org.junit.Before;
import org.junit.Test;
import org.omg.ETF.BufferHolder;
import org.omg.ETF.Profile;
import org.omg.GIOP.MsgType_1_1;

/**
 * GIOPConnectionTest.java
 *
 * @author Nicolas Noffke
 */
public class GIOPConnectionTest extends ORBTestCase
{
    private Configuration config;

    @Before
    public void setUp()
        throws Exception
    {
        config = JacORBConfiguration.getConfiguration(null, orb, false);
    }

    class DummyTransport extends org.omg.ETF._ConnectionLocalBase
    {
        private boolean closed = false;
        private byte[] data = null;
        private int index = 0;
        private ByteArrayOutputStream b_out = new ByteArrayOutputStream();
        private org.omg.ETF.Profile profile = new IIOPProfile
        (
            new IIOPAddress ("127.0.0.1", 4711),
            null,
            getORB().getGIOPMinorVersion()
        );

        public DummyTransport( List<byte[]> messages )
        {
            // convert the message list into a plain byte array

            int size = 0;
            for (Iterator<byte[]> i = messages.iterator(); i.hasNext();)
            {
                size += i.next().length;
            }
            data = new byte[size];
            int index = 0;
            for (Iterator<byte[]> i = messages.iterator(); i.hasNext();)
            {
                byte[] msg = i.next();
                System.arraycopy(msg, 0, data, index, msg.length);
                index += msg.length;
            }
        }

        public byte[] getWrittenMessage()
        {
            return b_out.toByteArray();
        }

        public void connect (org.omg.ETF.Profile profile, long time_out)
        {
            // nothing
        }

        public boolean hasBeenClosed()
        {
            return closed;
        }

        public boolean is_connected()
        {
            return !closed;
        }

        public void write( boolean is_first, boolean is_last,
                           byte[] message, int start, int size,
                           long timeout )
        {
            b_out.write( message, start, size );
        }


        public void flush()
        {
        }

        public void close()
        {
            closed = true;
        }

        public boolean isSSL()
        {
            return false;
        }

        public void turnOnFinalTimeout()
        {
        }

        public Profile get_server_profile()
        {
            return profile;
        }

        public int read (BufferHolder data, int offset,
                         int min_length, int max_length, long time_out)
        {
            if (this.index + min_length > this.data.length)
            {
                throw new org.omg.CORBA.COMM_FAILURE ("end of stream");
            }
            System.arraycopy(this.data, this.index, data.value, offset, min_length);
            this.index += min_length;
            return min_length;
        }

        public boolean is_data_available()
        {
            return true;
        }

        public boolean supports_callback()
        {
            return false;
        }

        public boolean use_handle_time_out()
        {
            return false;
        }

        public boolean wait_next_data(long time_out)
        {
            return false;
        }

    }


    private class DummyRequestListener
        implements RequestListener
    {
        private byte[] request = null;

        public DummyRequestListener()
        {
        }

        public byte[] getRequest()
        {
            return request;
        }

        public void requestReceived( byte[] request,
                                     GIOPConnection connection )
        {
            this.request = request;
        }

        public void locateRequestReceived( byte[] request,
                                           GIOPConnection connection )
        {
            this.request = request;
        }
        public void cancelRequestReceived( byte[] request,
                                           GIOPConnection connection )
        {
            this.request = request;
        }
    }

    private class DummyReplyListener
        implements ReplyListener
    {
        private byte[] reply = null;

        public DummyReplyListener()
        {
        }

        public byte[] getReply()
        {
            return reply;
        }

        public void replyReceived( byte[] reply,
                                   GIOPConnection connection )
        {
            this.reply = reply;
        }

        public void locateReplyReceived( byte[] reply,
                                         GIOPConnection connection )
        {
            this.reply = reply;
        }

        public void closeConnectionReceived( byte[] close_conn,
                                             GIOPConnection connection )
        {
            this.reply = close_conn;
        }

    }
    
    /**
     * Creates and returns a {@link org.jacorb.orb.giop.RequestOutputStream}, for creating a request message,
     *  and <code>num</code>-1 {@link org.jacorb.orb.giop.MessageOutputStream}s, for creating fragment messages.
     *
     * A giop message header is added to the messages.
     * A request header is added to the <code>RequestOutputStream</code>.
     * In case of giop minor version 2,
     *  a fragment header is added to the output streams for fragment messages.
     */
    public List<MessageOutputStream> createRequestOutputStreams(int num, int giopMinor)
    {
        List<MessageOutputStream> outputStreams = new ArrayList<MessageOutputStream>();
        outputStreams.add(new RequestOutputStream( getORB(),
                          (ClientConnection) null,
                          0,
                          "foo",       // operation
                          true,        // response expected
                          (short) -1,  // SYNC_SCOPE (irrelevant)
                          null,        // request start time
                          null,        // request end time
                          null,        // reply end time
                          new byte[1], // object key
                          giopMinor    // giop minor
                                                 ));
        for(int i=1; i< num ; i++)
        {
            MessageOutputStream m_out = new MessageOutputStream(orb);
            m_out.setGIOPMinor(giopMinor);
            m_out.writeGIOPMsgHeader( MsgType_1_1._Fragment, giopMinor);
            if(giopMinor > 1)
            {
                m_out.write_ulong( 0 ); // Fragment Header (request id), not present in GIOP 1.1
            }
            outputStreams.add(m_out);
        }
        return outputStreams;
    }

    /**
     * Inserts the message size in the given {@link org.jacorb.orb.giop.MessageOutputStream}s,
     * retrieves the message buffers, and sets the "more fragments follow" flag in each message buffer,
     * except in the last one.
     * The resulting message buffers are returned.
     */
    List<byte[]> completeAndRetrieveMessageBuffers(List<MessageOutputStream> outputStreams)
    {
        List<byte[]> messages = new Vector<byte[]>();

        for(int i=0; i<outputStreams.size(); i++)
        {
            MessageOutputStream outputStream = outputStreams.get(i);
            outputStream.insertMsgSize();
            byte[] message = outputStream.getBufferCopy();
            if(outputStreams.size() > 1 && i<outputStreams.size()-1)
            {
                message[6] |= 0x02; //set "more fragments follow"
            }
            messages.add(message);
        }
        return messages;
    }

    /**
     * Simulates retrieving the given messages, and returns the resulting
     *  {@link org.jacorb.org.giop.RequestInputStream}.
     */
    protected RequestInputStream receiveMessages(List<byte[]> messages)
    {
        DummyTransport transport = new DummyTransport( messages );

        DummyRequestListener request_listener = new DummyRequestListener();

        DummyReplyListener reply_listener = new DummyReplyListener();

        GIOPConnectionManager giopconn_mg =
            new GIOPConnectionManager();
        try
        {
            giopconn_mg.configure (config);
        }
        catch (Exception e)
        {
        }

        ServerGIOPConnection conn =
            giopconn_mg.createServerGIOPConnection( null,
                    transport,
                    request_listener,
                    reply_listener );

        try
        {
            //will not return until an IOException is thrown (by the
            //DummyTransport)
            conn.receiveMessages();
        }
        catch( IOException e )
        {
            //o.k., thrown by DummyTransport
        }
        catch( Exception e )
        {
            e.printStackTrace();
            fail( "Caught exception: " + e );
        }

        //did the GIOPConnection hand the complete request over to the
        //listener?
        assertTrue( request_listener.getRequest() != null );

        RequestInputStream r_in = new RequestInputStream
        ( orb, null, request_listener.getRequest() );
        return r_in;
    }    

    @Test
    public void testGIOP_1_1_readFragmented_String()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_ulong( 10 ); //string length
        outputStreams.get(0).write_octet( (byte) 'b' );
        outputStreams.get(0).write_octet( (byte) 'a' );
        outputStreams.get(0).write_octet( (byte) 'r' );

        outputStreams.get(1).write_octet( (byte) 'b' );
        outputStreams.get(1).write_octet( (byte) 'a' );
        outputStreams.get(1).write_octet( (byte) 'z' );

        outputStreams.get(2).write_octet( (byte) 'b' );
        outputStreams.get(2).write_octet( (byte) 'a' );
        outputStreams.get(2).write_octet( (byte) 'r' );
        outputStreams.get(2).write_octet( (byte) 0);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        String result = r_in.read_string();

        //is the body correct?
        assertEquals( "barbazbar", result);
    }

    @Test
    public void testGIOP_1_2_readFragmented_String()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_ulong( 10 ); //string length
        outputStreams.get(0).write_octet( (byte) 'b' );
        outputStreams.get(0).write_octet( (byte) 'a' );
        outputStreams.get(0).write_octet( (byte) 'r' );
        outputStreams.get(0).write_octet( (byte) 'b' );

        outputStreams.get(1).write_octet( (byte) 'a' );
        outputStreams.get(1).write_octet( (byte) 'z' );
        outputStreams.get(1).write_octet( (byte) 'b' );
        outputStreams.get(1).write_octet( (byte) 'a' );
        outputStreams.get(1).write_octet( (byte) 'r' );
        outputStreams.get(1).write_octet( (byte) 0);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        String result = r_in.read_string();

        //is the body correct?
        assertEquals("barbazbar", result);
    }

    @Test
    public void testGIOP_1_1_readFragmented_EmptyStringAndShort()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ulong( 1 ); //string length
        outputStreams.get(1).write_octet( (byte) 0); // string delimiter in next fragment
        outputStreams.get(1).write_short((short) 5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals("", r_in.read_string());
        assertEquals((short) 5, r_in.read_short());
    }

    @Test
    public void testGIOP_1_1_readFragmented_BooleanArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);

        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        boolean[] data = new boolean[6];
        r_in.read_boolean_array(data, 0, 6);

        //is the data correct?
        assertEquals( false, data[0]);
        assertEquals( false, data[1]);
        assertEquals( false, data[2]);
        assertEquals( true, data[3]);
        assertEquals( true, data[4]);
        assertEquals( true, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_Boolean()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);

        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        boolean[] data = new boolean[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_boolean();

        //is the data correct?
        assertEquals( false, data[0]);
        assertEquals( false, data[1]);
        assertEquals( false, data[2]);
        assertEquals( true, data[3]);
        assertEquals( true, data[4]);
        assertEquals( true, data[5]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_Boolean()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(false);

        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        boolean[] data = new boolean[19];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_boolean();

        //is the data correct?
        assertEquals( false, data[0]);
        assertEquals( false, data[1]);
        assertEquals( false, data[2]);
        assertEquals( true, data[3]);
        assertEquals( true, data[4]);
        assertEquals( true, data[5]);
        assertEquals( true, data[6]);
        assertEquals( false, data[7]);
        assertEquals( false, data[8]);
        assertEquals( false, data[9]);
        assertEquals( false, data[10]);
        assertEquals( true, data[11]);
        assertEquals( true, data[12]);
        assertEquals( true, data[13]);
        assertEquals( true, data[14]);
        assertEquals( false, data[15]);
        assertEquals( false, data[16]);
        assertEquals( false, data[17]);
        assertEquals( false, data[18]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_Char()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_char('a');
        outputStreams.get(0).write_char('b');
        outputStreams.get(0).write_char('c');
        outputStreams.get(0).write_char('d');
        outputStreams.get(0).write_char('e');
        outputStreams.get(0).write_char('f');
        outputStreams.get(0).write_char('g');
        outputStreams.get(0).write_char('h');

        outputStreams.get(1).write_char('i');
        outputStreams.get(1).write_char('j');
        outputStreams.get(1).write_char('k');
        outputStreams.get(1).write_char('l');
        outputStreams.get(1).write_char('m');
        outputStreams.get(1).write_char('n');
        outputStreams.get(1).write_char('o');
        outputStreams.get(1).write_char('p');
        outputStreams.get(1).write_char('q');
        outputStreams.get(1).write_char('r');
        outputStreams.get(1).write_char('s');

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[19];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_char();

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
        assertEquals( 'g', data[6]);
        assertEquals( 'h', data[7]);
        assertEquals( 'i', data[8]);
        assertEquals( 'j', data[9]);
        assertEquals( 'k', data[10]);
        assertEquals( 'l', data[11]);
        assertEquals( 'm', data[12]);
        assertEquals( 'n', data[13]);
        assertEquals( 'o', data[14]);
        assertEquals( 'p', data[15]);
        assertEquals( 'q', data[16]);
        assertEquals( 'r', data[17]);
        assertEquals( 's', data[18]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_CharArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_char('a');
        outputStreams.get(0).write_char('b');
        outputStreams.get(0).write_char('c');
        outputStreams.get(0).write_char('d');
        outputStreams.get(0).write_char('e');
        outputStreams.get(0).write_char('f');
        outputStreams.get(0).write_char('g');
        outputStreams.get(0).write_char('h');

        outputStreams.get(1).write_char('i');
        outputStreams.get(1).write_char('j');
        outputStreams.get(1).write_char('k');
        outputStreams.get(1).write_char('l');
        outputStreams.get(1).write_char('m');
        outputStreams.get(1).write_char('n');
        outputStreams.get(1).write_char('o');
        outputStreams.get(1).write_char('p');
        outputStreams.get(1).write_char('q');
        outputStreams.get(1).write_char('r');
        outputStreams.get(1).write_char('s');

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[19];

        r_in.read_char_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
        assertEquals( 'g', data[6]);
        assertEquals( 'h', data[7]);
        assertEquals( 'i', data[8]);
        assertEquals( 'j', data[9]);
        assertEquals( 'k', data[10]);
        assertEquals( 'l', data[11]);
        assertEquals( 'm', data[12]);
        assertEquals( 'n', data[13]);
        assertEquals( 'o', data[14]);
        assertEquals( 'p', data[15]);
        assertEquals( 'q', data[16]);
        assertEquals( 'r', data[17]);
        assertEquals( 's', data[18]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_Double()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        outputStreams.get(0).write_double(1.1);
        outputStreams.get(0).write_double(1.2);
        outputStreams.get(0).write_double(1.3);

        outputStreams.get(1).write_double(1.4);
        outputStreams.get(1).write_double(1.5);
        outputStreams.get(1).write_double(1.6);
        outputStreams.get(1).write_double(1.7);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        double[] data = new double[7];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_double();

        //is the data correct?
        assertEquals( 1.1 , data[0] , 1e-15);
        assertEquals( 1.2 , data[1] , 1e-15);
        assertEquals( 1.3 , data[2] , 1e-15);
        assertEquals( 1.4 , data[3] , 1e-15);
        assertEquals( 1.5 , data[4] , 1e-15);
        assertEquals( 1.6 , data[5] , 1e-15);
        assertEquals( 1.7 , data[6] , 1e-15);
    }

    @Test
    public void testGIOP_1_2_readFragmented_DoubleArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        outputStreams.get(0).write_double(1.1);
        outputStreams.get(0).write_double(1.2);
        outputStreams.get(0).write_double(1.3);

        outputStreams.get(1).write_double(1.4);
        outputStreams.get(1).write_double(1.5);
        outputStreams.get(1).write_double(1.6);
        outputStreams.get(1).write_double(1.7);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        double[] data = new double[7];

        r_in.read_double_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 1.1 , data[0], 1e-15);
        assertEquals( 1.2 , data[1], 1e-15);
        assertEquals( 1.3 , data[2], 1e-15);
        assertEquals( 1.4 , data[3], 1e-15);
        assertEquals( 1.5 , data[4], 1e-15);
        assertEquals( 1.6 , data[5], 1e-15);
        assertEquals( 1.7 , data[6], 1e-15);
    }

    @Test
    public void testGIOP_1_2_readFragmented_Float()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_float((float) 1.1);
        outputStreams.get(0).write_float((float) 1.2);
        outputStreams.get(0).write_float((float) 1.3);
        outputStreams.get(0).write_float((float) 1.4);

        outputStreams.get(1).write_float((float) 1.5);
        outputStreams.get(1).write_float((float) 1.6);
        outputStreams.get(1).write_float((float) 1.7);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        float[] data = new float[7];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_float();

        //is the data correct?
        assertEquals( (float) 1.1 , data[0] , 1e-15);
        assertEquals( (float) 1.2 , data[1] , 1e-15);
        assertEquals( (float) 1.3 , data[2] , 1e-15);
        assertEquals( (float) 1.4 , data[3] , 1e-15);
        assertEquals( (float) 1.5 , data[4] , 1e-15);
        assertEquals( (float) 1.6 , data[5] , 1e-15);
        assertEquals( (float) 1.7 , data[6] , 1e-15);
    }

    @Test
    public void testGIOP_1_2_readFragmented_FloatArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_float((float) 1.1);
        outputStreams.get(0).write_float((float) 1.2);
        outputStreams.get(0).write_float((float) 1.3);
        outputStreams.get(0).write_float((float) 1.4);

        outputStreams.get(1).write_float((float) 1.5);
        outputStreams.get(1).write_float((float) 1.6);
        outputStreams.get(1).write_float((float) 1.7);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        float[] data = new float[7];

        r_in.read_float_array(data, 0, data.length);

        //is the data correct?
        assertEquals( (float) 1.1 , data[0], 1e-15);
        assertEquals( (float) 1.2 , data[1], 1e-15);
        assertEquals( (float) 1.3 , data[2], 1e-15);
        assertEquals( (float) 1.4 , data[3], 1e-15);
        assertEquals( (float) 1.5 , data[4], 1e-15);
        assertEquals( (float) 1.6 , data[5], 1e-15);
        assertEquals( (float) 1.7 , data[6], 1e-15);
    }

    @Test
    public void testGIOP_1_2_readFragmented_Long()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_long(0);
        outputStreams.get(0).write_long(1);
        outputStreams.get(0).write_long(2);
        outputStreams.get(0).write_long(3);

        outputStreams.get(1).write_long(4);
        outputStreams.get(1).write_long(5);
        outputStreams.get(1).write_long(6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[7];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_long();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
        assertEquals( 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_LongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_long(0);
        outputStreams.get(0).write_long(1);
        outputStreams.get(0).write_long(2);
        outputStreams.get(0).write_long(3);

        outputStreams.get(1).write_long(4);
        outputStreams.get(1).write_long(5);
        outputStreams.get(1).write_long(6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[7];

        r_in.read_long_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
        assertEquals( 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_LongLong()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        outputStreams.get(0).write_longlong(0);
        outputStreams.get(0).write_longlong(1);
        outputStreams.get(0).write_longlong(2);

        outputStreams.get(1).write_longlong(3);
        outputStreams.get(1).write_longlong(4);
        outputStreams.get(1).write_longlong(5);
        outputStreams.get(1).write_longlong(6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[7];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_longlong();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
        assertEquals( 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_LongLongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        outputStreams.get(0).write_longlong(0);
        outputStreams.get(0).write_longlong(1);
        outputStreams.get(0).write_longlong(2);

        outputStreams.get(1).write_longlong(3);
        outputStreams.get(1).write_longlong(4);
        outputStreams.get(1).write_longlong(5);
        outputStreams.get(1).write_longlong(6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[7];

        r_in.read_longlong_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
        assertEquals( 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_BooleanArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(false);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_boolean(false);

        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(true);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);
        outputStreams.get(1).write_boolean(false);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        boolean[] data = new boolean[19];

        r_in.read_boolean_array(data, 0, data.length);

        //is the data correct?
        assertEquals( false, data[0]);
        assertEquals( false, data[1]);
        assertEquals( false, data[2]);
        assertEquals( true, data[3]);
        assertEquals( true, data[4]);
        assertEquals( true, data[5]);
        assertEquals( true, data[6]);
        assertEquals( false, data[7]);
        assertEquals( false, data[8]);
        assertEquals( false, data[9]);
        assertEquals( false, data[10]);
        assertEquals( true, data[11]);
        assertEquals( true, data[12]);
        assertEquals( true, data[13]);
        assertEquals( true, data[14]);
        assertEquals( false, data[15]);
        assertEquals( false, data[16]);
        assertEquals( false, data[17]);
        assertEquals( false, data[18]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_OctetArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_octet( (byte) 0);
        outputStreams.get(0).write_octet( (byte) 1);
        outputStreams.get(0).write_octet( (byte) 2);

        outputStreams.get(1).write_octet( (byte) 3);
        outputStreams.get(1).write_octet( (byte) 4);
        outputStreams.get(1).write_octet( (byte) 5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        byte[] data = new byte[6];
        r_in.read_octet_array(data, 0, 6);

        //is the data correct?
        assertEquals( (byte) 0, data[0]);
        assertEquals( (byte) 1, data[1]);
        assertEquals( (byte) 2, data[2]);
        assertEquals( (byte) 3, data[3]);
        assertEquals( (byte) 4, data[4]);
        assertEquals( (byte) 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_Octet()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_octet( (byte) 0);
        outputStreams.get(0).write_octet( (byte) 1);
        outputStreams.get(0).write_octet( (byte) 2);

        outputStreams.get(1).write_octet( (byte) 3);
        outputStreams.get(1).write_octet( (byte) 4);
        outputStreams.get(1).write_octet( (byte) 5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        byte[] data = new byte[6];

        for(int i = 0; i < data.length; i++)
            data[i] = r_in.read_octet();

        //is the data correct?
        assertEquals( (byte) 0, data[0]);
        assertEquals( (byte) 1, data[1]);
        assertEquals( (byte) 2, data[2]);
        assertEquals( (byte) 3, data[3]);
        assertEquals( (byte) 4, data[4]);
        assertEquals( (byte) 5, data[5]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_Octet()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        outputStreams.get(0).write_octet( (byte) 0);
        outputStreams.get(0).write_octet( (byte) 1);
        outputStreams.get(0).write_octet( (byte) 2);
        outputStreams.get(0).write_octet( (byte) 3);
        outputStreams.get(0).write_octet( (byte) 4);
        outputStreams.get(0).write_octet( (byte) 5);
        outputStreams.get(0).write_octet( (byte) 6);
        outputStreams.get(0).write_octet( (byte) 7);

        outputStreams.get(1).write_octet( (byte) 8);
        outputStreams.get(1).write_octet( (byte) 9);
        outputStreams.get(1).write_octet( (byte) 10);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        byte[] data = new byte[11];

        for(int i = 0; i < data.length; i++)
            data[i] = r_in.read_octet();

        //is the data correct?
        assertEquals( (byte) 0, data[0]);
        assertEquals( (byte) 1, data[1]);
        assertEquals( (byte) 2, data[2]);
        assertEquals( (byte) 3, data[3]);
        assertEquals( (byte) 4, data[4]);
        assertEquals( (byte) 5, data[5]);
        assertEquals( (byte) 6, data[6]);
        assertEquals( (byte) 7, data[7]);
        assertEquals( (byte) 8, data[8]);
        assertEquals( (byte) 9, data[9]);
        assertEquals( (byte) 10, data[10]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_OctetArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_octet( (byte) 0);
        outputStreams.get(0).write_octet( (byte) 1);
        outputStreams.get(0).write_octet( (byte) 2);
        outputStreams.get(0).write_octet( (byte) 3);
        outputStreams.get(0).write_octet( (byte) 4);
        outputStreams.get(0).write_octet( (byte) 5);
        outputStreams.get(0).write_octet( (byte) 6);
        outputStreams.get(0).write_octet( (byte) 7);

        outputStreams.get(1).write_octet( (byte) 8);
        outputStreams.get(1).write_octet( (byte) 9);
        outputStreams.get(1).write_octet( (byte) 10);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        byte[] data = new byte[11];

        r_in.read_octet_array(data, 0, data.length);

        //is the data correct?
        assertEquals( (byte) 0, data[0]);
        assertEquals( (byte) 1, data[1]);
        assertEquals( (byte) 2, data[2]);
        assertEquals( (byte) 3, data[3]);
        assertEquals( (byte) 4, data[4]);
        assertEquals( (byte) 5, data[5]);
        assertEquals( (byte) 6, data[6]);
        assertEquals( (byte) 7, data[7]);
        assertEquals( (byte) 8, data[8]);
        assertEquals( (byte) 9, data[9]);
        assertEquals( (byte) 10, data[10]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_Short()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_short( (short) 0);
        outputStreams.get(0).write_short( (short) 1);
        outputStreams.get(0).write_short( (short) 2);
        outputStreams.get(0).write_short( (short) 3);

        outputStreams.get(1).write_short( (short) 4);
        outputStreams.get(1).write_short( (short) 5);
        outputStreams.get(1).write_short( (short) 6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[7];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_short();

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
        assertEquals( (short) 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_ShortArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_short( (short) 0);
        outputStreams.get(0).write_short( (short) 1);
        outputStreams.get(0).write_short( (short) 2);
        outputStreams.get(0).write_short( (short) 3);

        outputStreams.get(1).write_short( (short) 4);
        outputStreams.get(1).write_short( (short) 5);
        outputStreams.get(1).write_short( (short) 6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[7];

        r_in.read_short_array(data, 0, data.length);

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
        assertEquals( (short) 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_uLong()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_ulong(0);
        outputStreams.get(0).write_ulong(1);

        outputStreams.get(1).write_ulong(2);
        outputStreams.get(1).write_ulong(3);
        outputStreams.get(1).write_ulong(4);
        outputStreams.get(1).write_ulong(5);
        outputStreams.get(1).write_ulong(6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[7];

        for(int i=0; i < data.length; i++)
            data[i]  = r_in.read_ulong();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
        assertEquals( 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_uLongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_ulong(0);
        outputStreams.get(0).write_ulong(1);

        outputStreams.get(1).write_ulong(2);
        outputStreams.get(1).write_ulong(3);
        outputStreams.get(1).write_ulong(4);
        outputStreams.get(1).write_ulong(5);
        outputStreams.get(1).write_ulong(6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[7];

        r_in.read_ulong_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
        assertEquals( 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_uLongLong()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_ulonglong(0);
        outputStreams.get(0).write_ulonglong(1);
        outputStreams.get(0).write_ulonglong(2);

        outputStreams.get(1).write_ulonglong(3);
        outputStreams.get(1).write_ulonglong(4);
        outputStreams.get(1).write_ulonglong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_ulonglong();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_uLongLongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_ulonglong(0);
        outputStreams.get(0).write_ulonglong(1);
        outputStreams.get(0).write_ulonglong(2);

        outputStreams.get(1).write_ulonglong(3);
        outputStreams.get(1).write_ulonglong(4);
        outputStreams.get(1).write_ulonglong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[6];

        r_in.read_ulonglong_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_ShortArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_short( (short) 0);
        outputStreams.get(0).write_short( (short) 1);
        outputStreams.get(0).write_short( (short) 2);

        outputStreams.get(1).write_short( (short) 3);
        outputStreams.get(1).write_short( (short) 4);
        outputStreams.get(1).write_short( (short) 5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[6];
        r_in.read_short_array(data, 0, 6);

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_Short()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_short( (short) 0);
        outputStreams.get(0).write_short( (short) 1);
        outputStreams.get(0).write_short( (short) 2);

        outputStreams.get(1).write_short( (short) 3);
        outputStreams.get(1).write_short( (short) 4);
        outputStreams.get(1).write_short( (short) 5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_short();

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_uShortArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ushort( (short) 0);
        outputStreams.get(0).write_ushort( (short) 1);
        outputStreams.get(0).write_ushort( (short) 2);

        outputStreams.get(1).write_ushort( (short) 3);
        outputStreams.get(1).write_ushort( (short) 4);
        outputStreams.get(1).write_ushort( (short) 5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[6];
        r_in.read_ushort_array(data, 0, 6);

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_uShort()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ushort( (short) 0);
        outputStreams.get(0).write_ushort( (short) 1);
        outputStreams.get(0).write_ushort( (short) 2);

        outputStreams.get(1).write_ushort( (short) 3);
        outputStreams.get(1).write_ushort( (short) 4);
        outputStreams.get(1).write_ushort( (short) 5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_ushort();

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_uShort()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_ushort( (short) 0);
        outputStreams.get(0).write_ushort( (short) 1);
        outputStreams.get(0).write_ushort( (short) 2);
        outputStreams.get(0).write_ushort( (short) 3);

        outputStreams.get(1).write_ushort( (short) 4);
        outputStreams.get(1).write_ushort( (short) 5);
        outputStreams.get(1).write_ushort( (short) 6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[7];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_ushort();

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
        assertEquals( (short) 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_uShortArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_ushort( (short) 0);
        outputStreams.get(0).write_ushort( (short) 1);
        outputStreams.get(0).write_ushort( (short) 2);
        outputStreams.get(0).write_ushort( (short) 3);

        outputStreams.get(1).write_ushort( (short) 4);
        outputStreams.get(1).write_ushort( (short) 5);
        outputStreams.get(1).write_ushort( (short) 6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        short[] data = new short[7];

        r_in.read_ushort_array(data, 0, data.length);

        //is the data correct?
        assertEquals( (short) 0, data[0]);
        assertEquals( (short) 1, data[1]);
        assertEquals( (short) 2, data[2]);
        assertEquals( (short) 3, data[3]);
        assertEquals( (short) 4, data[4]);
        assertEquals( (short) 5, data[5]);
        assertEquals( (short) 6, data[6]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_WChar()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_wchar('a');
        outputStreams.get(0).write_wchar('b');
        outputStreams.get(0).write_wchar('c');
        outputStreams.get(0).write_wchar('d');
        outputStreams.get(0).write_wchar('e');
        outputStreams.get(0).write_wchar('f');
        outputStreams.get(0).write_wchar('g');
        outputStreams.get(0).write_wchar('h');
        outputStreams.get(1).write_wchar('i');
        outputStreams.get(1).write_byte((byte) 0); //padding
        outputStreams.get(1).write_byte((byte) 0); //padding

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[9];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_wchar();

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
        assertEquals( 'g', data[6]);
        assertEquals( 'h', data[7]);
        assertEquals( 'i', data[8]);
    }

    @Test
    public void testGIOP_1_2_readFragmented_WCharArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        // in GIOP 1.2 and later, all fragment messages (except the last one) should have a length which is a multiple of 8
        outputStreams.get(0).write_wchar('a');
        outputStreams.get(0).write_wchar('b');
        outputStreams.get(0).write_wchar('c');
        outputStreams.get(0).write_wchar('d');
        outputStreams.get(0).write_wchar('e');
        outputStreams.get(0).write_wchar('f');
        outputStreams.get(0).write_wchar('g');
        outputStreams.get(0).write_wchar('h');
        outputStreams.get(1).write_wchar('i');
        outputStreams.get(1).write_byte((byte) 0); //padding
        outputStreams.get(1).write_byte((byte) 0); //padding

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[9];

        r_in.read_wchar_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
        assertEquals( 'g', data[6]);
        assertEquals( 'h', data[7]);
        assertEquals( 'i', data[8]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_LongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_long(0);
        outputStreams.get(0).write_long(1);
        outputStreams.get(0).write_long(2);

        outputStreams.get(1).write_long(3);
        outputStreams.get(1).write_long(4);
        outputStreams.get(1).write_long(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[6];
        r_in.read_long_array(data, 0, 6);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_Long()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_long(0);
        outputStreams.get(0).write_long(1);
        outputStreams.get(0).write_long(2);

        outputStreams.get(1).write_long(3);
        outputStreams.get(1).write_long(4);
        outputStreams.get(1).write_long(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_long();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_uLongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ulong(0);
        outputStreams.get(0).write_ulong(1);
        outputStreams.get(0).write_ulong(2);

        outputStreams.get(1).write_ulong(3);
        outputStreams.get(1).write_ulong(4);
        outputStreams.get(1).write_ulong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[6];
        r_in.read_ulong_array(data, 0, 6);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_uLong()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ulong(0);
        outputStreams.get(0).write_ulong(1);
        outputStreams.get(0).write_ulong(2);

        outputStreams.get(1).write_ulong(3);
        outputStreams.get(1).write_ulong(4);
        outputStreams.get(1).write_ulong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        int[] data = new int[6];

        for(int i=0; i < data.length; i++)
            data[i]  = r_in.read_ulong();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_LongLongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_longlong(0);
        outputStreams.get(0).write_longlong(1);
        outputStreams.get(0).write_longlong(2);

        outputStreams.get(1).write_longlong(3);
        outputStreams.get(1).write_longlong(4);
        outputStreams.get(1).write_longlong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[6];
        r_in.read_longlong_array(data, 0, 6);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_LongLong()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_longlong(0);
        outputStreams.get(0).write_longlong(1);
        outputStreams.get(0).write_longlong(2);

        outputStreams.get(1).write_longlong(3);
        outputStreams.get(1).write_longlong(4);
        outputStreams.get(1).write_longlong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_longlong();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_uLongLongArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        //manually write the first half of the string "barbaz"
        outputStreams.get(0).write_ulonglong(0);
        outputStreams.get(0).write_ulonglong(1);
        outputStreams.get(0).write_ulonglong(2);

        outputStreams.get(1).write_ulonglong(3);
        outputStreams.get(1).write_ulonglong(4);
        outputStreams.get(1).write_ulonglong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[6];
        r_in.read_ulonglong_array(data, 0, 6);

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_uLongLong()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ulonglong(0);
        outputStreams.get(0).write_ulonglong(1);
        outputStreams.get(0).write_ulonglong(2);

        outputStreams.get(1).write_ulonglong(3);
        outputStreams.get(1).write_ulonglong(4);
        outputStreams.get(1).write_ulonglong(5);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        long[] data = new long[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_ulonglong();

        //is the data correct?
        assertEquals( 0, data[0]);
        assertEquals( 1, data[1]);
        assertEquals( 2, data[2]);
        assertEquals( 3, data[3]);
        assertEquals( 4, data[4]);
        assertEquals( 5, data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_FloatArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_float((float) 1.1);
        outputStreams.get(0).write_float((float) 1.2);
        outputStreams.get(0).write_float((float) 1.3);

        outputStreams.get(1).write_float((float) 1.4);
        outputStreams.get(1).write_float((float) 1.5);
        outputStreams.get(1).write_float((float) 1.6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        float[] data = new float[6];
        r_in.read_float_array(data, 0, 6);

        //is the data correct?
        assertEquals( (float) 1.1 , data[0], 1e-15);
        assertEquals( (float) 1.2 , data[1], 1e-15);
        assertEquals( (float) 1.3 , data[2], 1e-15);
        assertEquals( (float) 1.4 , data[3], 1e-15);
        assertEquals( (float) 1.5 , data[4], 1e-15);
        assertEquals( (float) 1.6 , data[5], 1e-15);
    }

    @Test
    public void testGIOP_1_1_readFragmented_Float()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_float((float) 1.1);
        outputStreams.get(0).write_float((float) 1.2);
        outputStreams.get(0).write_float((float) 1.3);

        outputStreams.get(1).write_float((float) 1.4);
        outputStreams.get(1).write_float((float) 1.5);
        outputStreams.get(1).write_float((float) 1.6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        float[] data = new float[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_float();

        //is the data correct?
        assertEquals( (float) 1.1 , data[0], 1e-15);
        assertEquals( (float) 1.2 , data[1], 1e-15);
        assertEquals( (float) 1.3 , data[2], 1e-15);
        assertEquals( (float) 1.4 , data[3], 1e-15);
        assertEquals( (float) 1.5 , data[4], 1e-15);
        assertEquals( (float) 1.6 , data[5], 1e-15);
    }

    @Test
    public void testGIOP_1_1_readFragmented_DoubleArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_double(1.1);
        outputStreams.get(0).write_double(1.2);
        outputStreams.get(0).write_double(1.3);

        outputStreams.get(1).write_double(1.4);
        outputStreams.get(1).write_double(1.5);
        outputStreams.get(1).write_double(1.6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        double[] data = new double[6];
        r_in.read_double_array(data, 0, 6);

        //is the data correct?
        assertEquals( 1.1 , data[0], 1e-15);
        assertEquals( 1.2 , data[1], 1e-15);
        assertEquals( 1.3 , data[2], 1e-15);
        assertEquals( 1.4 , data[3], 1e-15);
        assertEquals( 1.5 , data[4], 1e-15);
        assertEquals( 1.6 , data[5], 1e-15);
    }

    @Test
    public void testGIOP_1_1_readFragmented_Double()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_double(1.1);
        outputStreams.get(0).write_double(1.2);
        outputStreams.get(0).write_double(1.3);

        outputStreams.get(1).write_double(1.4);
        outputStreams.get(1).write_double(1.5);
        outputStreams.get(1).write_double(1.6);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        double[] data = new double[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_double();

        //is the data correct?
        assertEquals( 1.1 , data[0], 1e-15);
        assertEquals( 1.2 , data[1], 1e-15);
        assertEquals( 1.3 , data[2], 1e-15);
        assertEquals( 1.4 , data[3], 1e-15);
        assertEquals( 1.5 , data[4], 1e-15);
        assertEquals( 1.6 , data[5], 1e-15);
    }

    @Test
    public void testGIOP_1_1_readFragmented_CharArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_char('a');
        outputStreams.get(0).write_char('b');
        outputStreams.get(0).write_char('c');

        outputStreams.get(1).write_char('d');
        outputStreams.get(1).write_char('e');
        outputStreams.get(1).write_char('f');

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[6];
        r_in.read_char_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_Char()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_char('a');
        outputStreams.get(0).write_char('b');
        outputStreams.get(0).write_char('c');

        outputStreams.get(1).write_char('d');
        outputStreams.get(1).write_char('e');
        outputStreams.get(1).write_char('f');

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_char();

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_WCharArray()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_wchar('a');
        outputStreams.get(0).write_wchar('b');
        outputStreams.get(0).write_wchar('c');

        outputStreams.get(1).write_wchar('d');
        outputStreams.get(1).write_wchar('e');
        outputStreams.get(1).write_wchar('f');

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[6];
        r_in.read_wchar_array(data, 0, data.length);

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
    }

    @Test
    public void testGIOP_1_1_readFragmented_WChar()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_wchar('a');
        outputStreams.get(0).write_wchar('b');
        outputStreams.get(0).write_wchar('c');

        outputStreams.get(1).write_wchar('d');
        outputStreams.get(1).write_wchar('e');
        outputStreams.get(1).write_wchar('f');

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        char[] data = new char[6];

        for(int i=0; i < data.length; i++)
            data[i] = r_in.read_wchar();

        //is the data correct?
        assertEquals( 'a', data[0]);
        assertEquals( 'b', data[1]);
        assertEquals( 'c', data[2]);
        assertEquals( 'd', data[3]);
        assertEquals( 'e', data[4]);
        assertEquals( 'f', data[5]);
    }

    @Test
    public void testGIOP_1_2_readString()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(1, 2);

        outputStreams.get(0).write_long(5);
        outputStreams.get(0).write_octet( (byte) 'a');
        outputStreams.get(0).write_octet( (byte) 'b');
        outputStreams.get(0).write_octet( (byte) 'c');
        outputStreams.get(0).write_octet( (byte) 'd');
        outputStreams.get(0).write_octet( (byte) 0);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        String result = r_in.read_string();

        //is the data correct?
        assertEquals( "abcd", result);
    }

    @Test
    public void testGIOP_1_1_readString()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(1, 1);

        outputStreams.get(0).write_long(5);
        outputStreams.get(0).write_octet( (byte) 'a');
        outputStreams.get(0).write_octet( (byte) 'b');
        outputStreams.get(0).write_octet( (byte) 'c');
        outputStreams.get(0).write_octet( (byte) 'd');
        outputStreams.get(0).write_octet( (byte) 0);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        String result = r_in.read_string();

        //is the data correct?
        assertEquals( "abcd", result);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes1248(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_boolean(true);
        outputStreams.get(0).write_short((short) 2);
        outputStreams.get(0).write_long(5);
        if(addPadding)
        {
            // add padding bytes (the amount of padding which would be needed
            //  if the next double would still be written to this fragment)
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_double(9);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(true, r_in.read_boolean());
        assertEquals((short) 2, r_in.read_short());
        assertEquals((long) 5, r_in.read_long());
        assertEquals(new Double(9), new Double(r_in.read_double()));
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1248_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1248(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1248_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1248(false);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1284() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_wchar('a');
        outputStreams.get(0).write_ushort((short) 2);
        assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        outputStreams.get(1).write_longlong(99);
        assertTrue(outputStreams.get(1).get_pos() % 4 == 0);
        outputStreams.get(2).write_float((float)5.1);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals('a', r_in.read_wchar());
        assertEquals((short) 2, r_in.read_ushort());
        assertEquals(99, r_in.read_longlong());
        assertEquals(new Float(5.1), new Float(r_in.read_float()));
    }

    public void helpTestGIOP_1_1_readFragmented_sizes1428(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_octet((byte) 0xfe);
        outputStreams.get(0).write_ulong(123456789);
        outputStreams.get(0).write_short((short) 1244);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(9999776655443322L);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((byte) 0xfe, r_in.read_octet());
        assertEquals(123456789, r_in.read_ulong());
        assertEquals((short) 1244, r_in.read_short());
        assertEquals(9999776655443322L, r_in.read_longlong());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1428_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1428(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1428_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1428(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes1482(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_octet((byte) 0xab);
        outputStreams.get(0).write_long(-234567891);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 2 == 0);
        outputStreams.get(2).write_ushort((short) 61111);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(-234567891, r_in.read_long());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((short) 61111, r_in.read_ushort());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1482_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1482(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1482_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1482(false);
    }
    
    public void helpTestGIOP_1_1_readFragmented_sizes1824(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_octet((byte) 0xab);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 2 == 0);
        outputStreams.get(2).write_ushort((short) 61111);
        outputStreams.get(2).write_long(-234567891);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(-234567891, r_in.read_long());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1824_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1824(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1824_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1824(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes1842(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_octet((byte) 0xab);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 4 == 0);
        outputStreams.get(2).write_long(-234567891);
        outputStreams.get(2).write_ushort((short) 61111);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals(-234567891, r_in.read_long());
        assertEquals((short) 61111, r_in.read_ushort());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1842_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1842(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes1842_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes1842(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes2148(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ushort((short) 61111);
        outputStreams.get(0).write_octet((byte) 0xab);
        outputStreams.get(0).write_long(-234567891);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(-234567891, r_in.read_long());
        assertEquals(123456789876L, r_in.read_longlong());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2148_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2148(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2148_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2148(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes2184(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_ushort((short) 61111);
        outputStreams.get(0).write_octet((byte) 0xab);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 4 == 0);
        outputStreams.get(2).write_long(-234567891);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals(-234567891, r_in.read_long());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2184_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2184(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2184_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2184(false);
    }
       
    public void helpTestGIOP_1_1_readFragmented_sizes2418(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_ushort((short) 61111);
        outputStreams.get(0).write_long(-234567891);
        outputStreams.get(0).write_octet((byte) 0xab);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(-234567891, r_in.read_long());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(123456789876L, r_in.read_longlong());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2418_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2418(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2418_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2418(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes2481(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_ushort((short) 61111);
        outputStreams.get(0).write_long(-234567891);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        outputStreams.get(2).write_octet((byte) 0xab);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(-234567891, r_in.read_long());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((byte) 0xab, r_in.read_octet());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2481_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2481(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2481_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2481(false);
    }
 
    public void helpTestGIOP_1_1_readFragmented_sizes2841(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_ushort((short) 61111);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 4 == 0);
        outputStreams.get(2).write_long(-234567891);
        outputStreams.get(2).write_octet((byte) 0xab);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals(-234567891, r_in.read_long());
        assertEquals((byte) 0xab, r_in.read_octet());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2841_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2841(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2841_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2841(false);
    }

    private void helpTestGIOP_1_1_readFragmented_sizes2814(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_ushort((short) 61111);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        outputStreams.get(2).write_octet((byte) 0xab);
        outputStreams.get(2).write_long(-234567891);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(-234567891, r_in.read_long());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2814_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2814(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes2814_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes2814(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes4128(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_long(-234567891);
        outputStreams.get(0).write_octet((byte) 0xab);
        outputStreams.get(0).write_ushort((short) 61111);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(-234567891, r_in.read_long());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(123456789876L, r_in.read_longlong());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes4128_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4128(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes4128_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4128(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes4182(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_long(-234567891);
        outputStreams.get(0).write_octet((byte) 0xab);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 2 == 0);
        outputStreams.get(2).write_ushort((short) 61111);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(-234567891, r_in.read_long());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((short) 61111, r_in.read_ushort());
    }
    
    @Test
    public void testGIOP_1_1_readFragmented_sizes4182_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4182(true);
    }

    @Test    
    public void testGIOP_1_1_readFragmented_sizes4182_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4182(false);
    }

    public void helpTestGIOP_1_1_readFragmented_sizes4281(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_long(-234567891);
        outputStreams.get(0).write_ushort((short) 61111);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        outputStreams.get(2).write_octet((byte) 0xab);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(-234567891, r_in.read_long());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((byte) 0xab, r_in.read_octet());
    }

    @Test    
    public void testGIOP_1_1_readFragmented_sizes4281_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4281(true);
    }

    @Test    
    public void testGIOP_1_1_readFragmented_sizes4281_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4281(false);
    }
  
    public void helpTestGIOP_1_1_readFragmented_sizes4218(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_long(-234567891);
        outputStreams.get(0).write_ushort((short) 61111);
        outputStreams.get(0).write_octet((byte) 0xab);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(-234567891, r_in.read_long());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(123456789876L, r_in.read_longlong());
    }
    
    @Test
    public void testGIOP_1_1_readFragmented_sizes4218_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4218(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes4218_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes4218(false);
    }

    public void testGIOP_1_1_readFragmented_sizes4821() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_long(-234567891);
        assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 2 == 0);
        outputStreams.get(2).write_ushort((short) 61111);
        outputStreams.get(2).write_octet((byte) 0xab);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(-234567891, r_in.read_long());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals((byte) 0xab, r_in.read_octet());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes4812() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_long(-234567891);
        assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        outputStreams.get(1).write_longlong(123456789876L);
        outputStreams.get(2).write_octet((byte) 0xab);
        outputStreams.get(2).write_ushort((short) 61111);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(-234567891, r_in.read_long());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals((short) 61111, r_in.read_ushort());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes8124() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_longlong(123456789876L);
        assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        outputStreams.get(1).write_octet((byte) 0xab);
        outputStreams.get(1).write_ushort((short) 61111);
        outputStreams.get(1).write_long(-234567891);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(-234567891, r_in.read_long());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes8142() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_longlong(123456789876L);
        outputStreams.get(1).write_octet((byte) 0xab);
        outputStreams.get(1).write_long(-234567891);
        outputStreams.get(1).write_ushort((short) 61111);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(-234567891, r_in.read_long());
        assertEquals((short) 61111, r_in.read_ushort());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes8241() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_longlong(123456789876L);
        outputStreams.get(1).write_ushort((short) 61111);
        outputStreams.get(1).write_long(-234567891);
        outputStreams.get(1).write_octet((byte) 0xab);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(-234567891, r_in.read_long());
        assertEquals((byte) 0xab, r_in.read_octet());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes8214() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_longlong(123456789876L);
        outputStreams.get(1).write_ushort((short) 61111);
        outputStreams.get(1).write_octet((byte) 0xab);
        outputStreams.get(1).write_long(-234567891);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals(-234567891, r_in.read_long());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes8421() throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 1);

        outputStreams.get(0).write_longlong(123456789876L);
        outputStreams.get(1).write_long(-234567891);
        outputStreams.get(1).write_ushort((short) 61111);
        outputStreams.get(1).write_octet((byte) 0xab);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals(-234567891, r_in.read_long());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals((byte) 0xab, r_in.read_octet());
    }

    public void helpTestGIOP_1_1_readFragmented_sizes12488(boolean addPadding) throws Exception
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(3, 1);

        outputStreams.get(0).write_octet((byte) 0xab);
        outputStreams.get(0).write_ushort((short) 61111);
        outputStreams.get(0).write_long(-234567891);
        if(addPadding)
        {
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            outputStreams.get(0).write_byte((byte) 0);
            assertTrue(outputStreams.get(0).get_pos() % 8 == 0);
        }
        outputStreams.get(1).write_longlong(123456789876L);
        assertTrue(outputStreams.get(1).get_pos() % 8 == 0);
        outputStreams.get(2).write_longlong(234567898765L);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        assertEquals((byte) 0xab, r_in.read_octet());
        assertEquals((short) 61111, r_in.read_ushort());
        assertEquals(-234567891, r_in.read_long());
        assertEquals(123456789876L, r_in.read_longlong());
        assertEquals(234567898765L, r_in.read_longlong());
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes12488_withPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes12488(true);
    }

    @Test
    public void testGIOP_1_1_readFragmented_sizes12488_noPadding() throws Exception
    {
        helpTestGIOP_1_1_readFragmented_sizes12488(false);
    }

    @Test
    public void testGIOP_1_2_CorrectFragmentedRequest()
    {
        List<MessageOutputStream> outputStreams = createRequestOutputStreams(2, 2);

        //manually write the first half of the string "barbaz"
        outputStreams.get(0).write_ulong( 7 ); //string length
        outputStreams.get(0).write_octet( (byte) 'b' );
        outputStreams.get(0).write_octet( (byte) 'a' );
        outputStreams.get(0).write_octet( (byte) 'r' );

        outputStreams.get(1).write_octet( (byte) 'b' );
        outputStreams.get(1).write_octet( (byte) 'a' );
        outputStreams.get(1).write_octet( (byte) 'z' );
        outputStreams.get(1).write_octet( (byte) 0);

        RequestInputStream r_in = receiveMessages(completeAndRetrieveMessageBuffers(outputStreams));

        //is the body correct?
        assertEquals( "barbaz", r_in.read_string() );
    }

    @Test
    public void testGIOP_1_0_CorrectRefusing()
    {
        List<byte[]> messages = new Vector<byte[]>();

        RequestOutputStream r_out =
            new RequestOutputStream( getORB(), //ClientConnection
                                     null,           //request id
                                     0,       //operation
                                     "foo",        //response expected
                                     true,   //SYNC_SCOPE (irrelevant)
                                     (short)-1,        //request start time
                                     null,        //request end time
                                     null,        //reply end time
                                     null, //object key
                                     new byte[1], 0            // giop minor
                                   );

        r_out.write_string( "bar" );
        r_out.insertMsgSize();

        byte[] b = r_out.getBufferCopy();

        b[6] |= 0x02; //set "more fragments follow"

        messages.add( b );

        DummyTransport transport =
            new DummyTransport( messages );

        DummyRequestListener request_listener =
            new DummyRequestListener();

        DummyReplyListener reply_listener =
            new DummyReplyListener();

        GIOPConnectionManager giopconn_mg =
            new GIOPConnectionManager();
        try
        {
            giopconn_mg.configure (config);
        }
        catch (Exception e)
        {
        }

        GIOPConnection conn =
            giopconn_mg.createServerGIOPConnection( null,
                    transport,
                    request_listener,
                    reply_listener );

        try
        {
            //will not return until an IOException is thrown (by the
            //DummyTransport)
            conn.receiveMessages();
        }
        catch( IOException e )
        {
            //o.k., thrown by DummyTransport
        }
        catch( Exception e )
        {
            e.printStackTrace();
            fail( "Caught exception: " + e );
        }

        //no request or reply must have been handed over
        assertTrue( request_listener.getRequest() == null );
        assertTrue( reply_listener.getReply() == null );

        //instead, an error message have must been sent via the
        //transport
        assertTrue( transport.getWrittenMessage() != null );

        byte[] result = transport.getWrittenMessage();

        assertTrue( Messages.getMsgType( result ) == MsgType_1_1._MessageError );
        MessageOutputStream m_out =
            new MessageOutputStream(orb);
        m_out.writeGIOPMsgHeader( MsgType_1_1._Fragment,
                                  0 // giop minor
                                );
        m_out.write_ulong( 0 ); // Fragment Header (request id)
        m_out.write_octet( (byte) 'b' );
        m_out.write_octet( (byte) 'a' );
        m_out.write_octet( (byte) 'z' );
        m_out.insertMsgSize();

        messages.add( m_out.getBufferCopy() );

        try
        {
            //will not return until an IOException is thrown (by the
            //DummyTransport)
            conn.receiveMessages();
        }
        catch( IOException e )
        {
            //o.k., thrown by DummyTransport
        }
        catch( Exception e )
        {
            e.printStackTrace();
            fail( "Caught exception: " + e );
        }

        //no request or reply must have been handed over
        assertTrue( request_listener.getRequest() == null );
        assertTrue( reply_listener.getReply() == null );

        //instead, an error message have must been sent via the
        //transport
        assertTrue( transport.getWrittenMessage() != null );

        //must be a new one
        assertTrue( transport.getWrittenMessage() != result );
        result = transport.getWrittenMessage();

        assertTrue( Messages.getMsgType( result ) == MsgType_1_1._MessageError );

    }

    @Test
    public void testGIOP_1_1_IllegalMessageType()
    {
        List<byte[]> messages = new Vector<byte[]>();

        LocateRequestOutputStream r_out =
            new LocateRequestOutputStream(
            orb,
            new byte[1], //object key
            0,           //request id
            1            // giop minor
        );

        r_out.insertMsgSize();

        byte[] b = r_out.getBufferCopy();

        b[6] |= 0x02; //set "more fragments follow"

        messages.add( b );

//        MessageOutputStream m_out =
//            new MessageOutputStream();

        DummyTransport transport =
            new DummyTransport( messages );

        DummyRequestListener request_listener =
            new DummyRequestListener();

        DummyReplyListener reply_listener =
            new DummyReplyListener();

        GIOPConnectionManager giopconn_mg =
            new GIOPConnectionManager();
        try
        {
            giopconn_mg.configure (config);
        }
        catch (Exception e)
        {
        }

        GIOPConnection conn =
            giopconn_mg.createServerGIOPConnection( null,
                    transport,
                    request_listener,
                    reply_listener );

        try
        {
            //will not return until an IOException is thrown (by the
            //DummyTransport)
            conn.receiveMessages();
        }
        catch( IOException e )
        {
            //o.k., thrown by DummyTransport
        }
        catch( Exception e )
        {
            e.printStackTrace();
            fail( "Caught exception: " + e );
        }

        //no request or reply must have been handed over
        assertTrue( request_listener.getRequest() == null );
        assertTrue( reply_listener.getReply() == null );

        //instead, an error message have must been sent via the
        //transport
        assertTrue( transport.getWrittenMessage() != null );

        byte[] result = transport.getWrittenMessage();

        assertTrue( Messages.getMsgType( result ) == MsgType_1_1._MessageError );
    }

    @Test
    public void testGIOP_1_2_CorrectCloseOnGarbage()
    {
        List<byte[]> messages = new Vector<byte[]>();

        String garbage = "This is a garbage message";
        byte[] b = garbage.getBytes();

        messages.add( b );

        DummyTransport transport =
            new DummyTransport( messages );

        DummyRequestListener request_listener =
            new DummyRequestListener();

        DummyReplyListener reply_listener =
            new DummyReplyListener();

        GIOPConnectionManager giopconn_mg =
            new GIOPConnectionManager();
        try
        {
            giopconn_mg.configure (config);
        }
        catch (Exception e)
        {
        }

        GIOPConnection conn =
            giopconn_mg.createServerGIOPConnection( null,
                    transport,
                    request_listener,
                    reply_listener );

        try
        {
            //will not return until an IOException is thrown (by the
            //DummyTransport)
            conn.receiveMessages();
        }
        catch( IOException e )
        {
            //o.k., thrown by DummyTransport
        }
        catch( Exception e )
        {
            e.printStackTrace();
            fail( "Caught exception: " + e );
        }

        //no request or reply must have been handed over
        assertTrue( request_listener.getRequest() == null );
        assertTrue( reply_listener.getReply() == null );

        //instead, connection should be closed
        assertTrue( transport.hasBeenClosed() );

        //no message is written (makes no real sense)
        assertTrue( transport.getWrittenMessage() != null );
        assertTrue( transport.getWrittenMessage().length == 0 );
    }

    @Test
    public void testGIOP_1_1_CorrectRequest()
    {
        List<byte[]> messages = new Vector<byte[]>();

        RequestOutputStream r_out =
            new RequestOutputStream( getORB(), //ClientConnection
                                     (ClientConnection) null,           //request id
                                     0,       //operation
                                     "foo",        // response expected
                                     true,   // SYNC_SCOPE (irrelevant)
                                     (short)-1,        //request start time
                                     null,        //request end time
                                     null,        //reply start time
                                     null, //object key
                                     new byte[1], 1            // giop minor
                                   );

        String message = "Request";
        r_out.write_string(message);
        r_out.insertMsgSize();

        messages.add( r_out.getBufferCopy() );

        RequestInputStream r_in = receiveMessages(messages);

        //is the body correct?
        assertEquals( message, r_in.read_string() );
    }    
}// GIOPConnectionTest
