package demo; 
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util; 

public class demo extends Applet {
    final static byte CLA_APPLET = (byte) 0xA0;     
    final static byte INS_REGISTER = (byte) 0x01; 
    final static byte INS_VERIFY = (byte) 0x02;   
    final static byte INS_GET_INFO = (byte) 0x03; 
    final static byte INS_CHANGE_PIN = (byte) 0x04;
    final static byte INS_UNBLOCK_PIN = (byte) 0x05;

    final static byte PIN_TRY_LIMIT = (byte) 0x03; 
    final static byte MAX_PIN_SIZE = (byte) 0x06;  
    
    private OwnerPIN pin;     
    private byte[] userData; 
    private short userDataLen; 
    final static short MAX_DATA_SIZE = (short) 256;     

    public static void install(byte[] bArray, short bOffset, byte bLength) {         
        new demo().register(bArray, (short) (bOffset + 1), bArray[bOffset]);     
    }     

    protected demo() {
		pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);                  
        userData = new byte[MAX_DATA_SIZE];         
        userDataLen = 0;     
    }     

    public void process(APDU apdu) {         
        if (selectingApplet()) { return; }         
        byte[] buf = apdu.getBuffer();                  
        
        if (buf[ISO7816.OFFSET_CLA] != CLA_APPLET) {             
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);         
        }         
        
        switch (buf[ISO7816.OFFSET_INS]) {             
            case INS_REGISTER:                 
                registerUser(apdu);                 
                break;             
            case INS_VERIFY:                 
                verifyPin(apdu);                 
                break;             
            case INS_GET_INFO:                 
                getInfo(apdu);                 
                break;
            case INS_CHANGE_PIN:
                changePin(apdu);
                break;
            case INS_UNBLOCK_PIN:
                resetPin(apdu);
                break;
            default:                 
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);         
        }     
    }     

    private void changePin(APDU apdu) {
        if (!pin.isValidated()) {
             ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buf = apdu.getBuffer();
        byte len = (byte)apdu.setIncomingAndReceive();
        
        if (len > MAX_PIN_SIZE || len < 1) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        pin.update(buf, ISO7816.OFFSET_CDATA, len);
    }
    
    private void resetPin(APDU apdu) {
        byte[] defaultPIN = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36};
        pin.update(defaultPIN, (short)0, (byte)6);
        pin.resetAndUnblock();
    }
    
    private void registerUser(APDU apdu) {         
        byte[] buf = apdu.getBuffer();         
        short len = apdu.setIncomingAndReceive(); 
        byte pinLen = buf[ISO7816.OFFSET_CDATA];                  
        if (pinLen > MAX_PIN_SIZE || pinLen <= 0) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);         
        pin.update(buf, (short)(ISO7816.OFFSET_CDATA + 1), pinLen);                  
        short dataOffset = (short)(ISO7816.OFFSET_CDATA + 1 + pinLen);         
        short dataLen = (short)(len - 1 - pinLen);                  
        if (dataLen > MAX_DATA_SIZE) ISOException.throwIt(ISO7816.SW_FILE_FULL);         
        Util.arrayCopy(buf, dataOffset, userData, (short)0, dataLen);         
        userDataLen = dataLen; 
    }     

    private void verifyPin(APDU apdu) {         
        byte[] buf = apdu.getBuffer();         
        short len = apdu.setIncomingAndReceive();                  
        if (pin.check(buf, ISO7816.OFFSET_CDATA, (byte)len) == false) {             
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);         
        }     
    }     

    private void getInfo(APDU apdu) {         
        if (!pin.isValidated()) ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);         
        apdu.setOutgoing();         
        apdu.setOutgoingLength(userDataLen);                  
        apdu.sendBytesLong(userData, (short)0, userDataLen);     
    } 
}