package de.caluga.morphium.driver.bson;/**
 * Created by stephan on 27.10.15.
 */

import de.caluga.morphium.Logger;

import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: Add Documentation here
 **/
public class MongoId {

    private final static int THE_MACHINE_ID;
    private int machineId;
    private static final AtomicInteger COUNT = new AtomicInteger(new SecureRandom().nextInt());
    private final short pid;
    private final int counter;
    private final int timestamp;

    public static ThreadLocal<Short> threadPid;

    static {
        try {
            THE_MACHINE_ID = createMachineId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MongoId() {
        long start = System.currentTimeMillis();

        pid = createPID();
        long dur = System.currentTimeMillis() - start;
        counter = COUNT.getAndIncrement() & 0x00ffffff;
        timestamp = (int) (System.currentTimeMillis() / 1000);
        machineId = THE_MACHINE_ID;
    }

    private MongoId(short pid, int c, int ts) {
        this.pid = pid;
        counter = c;
        timestamp = ts;
        machineId = THE_MACHINE_ID;
    }

    public MongoId(byte[] bytes, int idx) {
        if (bytes == null) {
            throw new IllegalArgumentException();
        } else if (idx + 12 >= bytes.length) {
            throw new IllegalArgumentException("not enough data, 12 bytes needed");
        } else {
            this.timestamp = readInt(bytes, idx);
            this.machineId = readInt(new byte[]{(byte) 0, bytes[idx + 4], bytes[idx + 5], bytes[idx + 6]}, 0);
            this.pid = (short) readInt(new byte[]{(byte) 0, (byte) 0, bytes[idx + 7], bytes[idx + 8]}, 0);
            this.counter = readInt(new byte[]{(byte) 0, bytes[idx + 9], bytes[idx + 10], bytes[idx + 11]}, 0);
        }
    }

    private int readInt(byte[] bytes, int idx) {
        return bytes[idx] << 24 | (bytes[idx + 1] & 0xFF) << 16 | (bytes[idx + 2] & 0xFF) << 8 | (bytes[idx + 3] & 0xFF);

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MongoId)) return false;

        MongoId mongoId = (MongoId) o;

        if (machineId != mongoId.machineId) return false;
        if (pid != mongoId.pid) return false;
        if (counter != mongoId.counter) return false;
        return timestamp == mongoId.timestamp;

    }

    @Override
    public int hashCode() {
        int result = machineId;
        result = 31 * result + (int) pid;
        result = 31 * result + counter;
        result = 31 * result + timestamp;
        return result;
    }

    private void storeInt(byte[] arr, int offset, int val) {
        for (int i = 0; i < 4; i++) arr[i + offset] = ((byte) ((val >> ((7 - i) * 8)) & 0xff));
    }

    private void storeShort(byte[] arr, int offset, int val) {
        arr[offset] = ((byte) ((val >> 8) & 0xff));
        arr[offset + 1] = ((byte) ((val) & 0xff));
    }

    private void storeInt3Byte(byte[] arr, int offset, int val) {
        arr[offset] = ((byte) ((val >> 16) & 0xff));
        arr[offset + 1] = ((byte) ((val >> 8) & 0xff));
        arr[offset + 2] = ((byte) ((val) & 0xff));
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[12];

        storeInt(bytes, 0, timestamp);
        storeInt3Byte(bytes, 4, machineId);
        storeShort(bytes, 7, pid);
        storeInt3Byte(bytes, 9, counter);
        return bytes;
    }

    private static short createPID() {
        if (threadPid == null || threadPid.get() == null) {

            short processId;
            try {
                String pName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                if (pName.contains("@")) {
                    processId = (short) Integer.parseInt(pName.substring(0, pName.indexOf('@')));
                } else {
                    processId = (short) pName.hashCode();
                }
            } catch (Throwable t) {
                new Logger(MongoId.class).error("could not get processID - using random fallback");
                processId = (short) new SecureRandom().nextInt();
            }
            threadPid = new ThreadLocal<>();
            threadPid.set(processId);
        }

        return threadPid.get();
    }


    private static int createMachineId() {
        int machineId = 0;
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            StringBuilder b = new StringBuilder();

            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                byte[] hwAdress = ni.getHardwareAddress();
                if (hwAdress != null) {
                    ByteBuffer buf = ByteBuffer.wrap(hwAdress);
                    try {
                        b.append(buf.getChar());
                        b.append(buf.getChar());
                        b.append(buf.getChar());
                    } catch (BufferUnderflowException shortHardwareAddressException) {
                        //cannot be
                    }
                }
            }
            machineId = b.toString().hashCode();
        } catch (Throwable t) {
            new Logger(MongoId.class).error("error accessing nics to create machine identifier... using fallback", t);
        }

        if (machineId == 0) machineId = (new SecureRandom().nextInt());

        machineId = machineId & 0x00ffffff;
        return machineId;
    }


    public String toString() {
        String[] chars = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",};
        byte[] b = getBytes();

        StringBuilder bld = new StringBuilder();

        for (byte by : b) {
            int idx = (by >>> 4) & 0x0f;
            bld.append(chars[idx]);
            idx = by & 0x0f;
            bld.append(chars[idx]);
        }
        return bld.toString();
    }
}