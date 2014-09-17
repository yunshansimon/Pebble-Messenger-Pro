package yangtsao.pebblemessengerpro.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yunshansimon on 14-8-19.
 */
public class CharacterMatrix {
    private  List<Byte> _byteList;
    private  int        _widthBytes;
    private  byte[]     _pos;

    // the row,col position of the character at the pebble screen(10,9~18)

    public  CharacterMatrix(String hex) {
        // 0.03125 sets the ration between the length of the hex representation
        // (64 or 32 char)
        // versus character width (2 or 1 byte respectively)
        this._widthBytes = (int) (hex.length() * 0.03125);
        this._pos = new byte[2];
        // Array is hex, two hex chars => byte, so the size is /2
        this._byteList = new ArrayList<Byte>(hex.length() / 2);

        if (this._widthBytes == 2) {
            while (hex.length() > 0) {
                String temp = hex.substring(0, 4);

                hex = hex.substring(4);

                int value = Integer.parseInt(temp, 16);

                byte[] end = new byte[2];

                // Convert value to two bytes (always fits 0x0 to 0xFFFF)
                end[1] = (byte) (value & 0xFF);
                end[0] = (byte) ((value >>> 8) & 0xFF);

                // Reverse the 1s and 0s to write black on white bg
                //end[1] = (byte) (~end[1] & 0xff);
                //end[0] = (byte) (~end[0] & 0xff);

                end[1] = (byte) (Integer.reverse(end[1]) >>> 24);
                end[0] = (byte) (Integer.reverse(end[0]) >>> 24);

                this._byteList.add(end[0]);
                this._byteList.add(end[1]);

            }
        } else if (this._widthBytes == 1) {
            while (hex.length() > 0) {
                String temp = hex.substring(0, 2);

                hex = hex.substring(2);

                int value = Integer.parseInt(temp, 16);

                byte end = (byte) (value & 0xFF);

                end = (byte) (Integer.reverse(end) >>> 24);

                this._byteList.add(end);
            }
        }
    }

    public byte[] getPos() {
        return _pos;
    }

    public void setPos(int row, int col) {

        this._pos[0] = (byte) row;
        this._pos[1] = (byte) col;

    }

    public List<Byte> getByteList() {
        return _byteList;
    }

    public int getWidthBytes() {
        return _widthBytes;
    }

    public void getbyteArray(byte[] buff, int size) {
        for (int i = 0; i < size; i++) {
            buff[i] = _byteList.get(i);
        }
    }
/*
   public String x_getBinaryRepresentation() {
        Byte[] bytes = new Byte[_byteList.size()];
        getByteList().toArray(bytes);

        return PebbleMessage.toBinary(bytes);
    }
    */
}
