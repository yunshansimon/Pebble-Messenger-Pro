package yangtsao.pebblemessengerpro.models;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by yunshansimon on 14-8-19.
 */
public class PebbleCall implements Serializable {


    private static final long serialVersionUID = 3353633765259495217L;
    private Deque<CharacterMatrix> _characterQueue;
    /* store the unicode character's imgs and their positions */
    private String                 _ascmsg;
    private String                 _phonenum;
    private Long                   _id;

    public Long get_id() {
        return _id;
    }

    public void set_id(Long id) {
        if (id<0) {
            this._id =(long) 0;
        }else {
            this._id = id;
        }
    }

    public String getAscMsg() {
        return _ascmsg;
    }

    public void setAscMsg(String msg) {
        this._ascmsg = msg;
    }

    public String getPhoneNum() {
        return _phonenum;
    }

    public void setPhoneNum(String num) {
        this._phonenum = num;
    }

    public void AddCharToAscMsg(char c) {
        this._ascmsg += String.valueOf(c);

    }

    public void AddStringToAscMsg(String s) {
        this._ascmsg += s;
    }

    public PebbleCall(Deque<CharacterMatrix> characterQueue) {
        this._characterQueue = characterQueue;
    }

    public PebbleCall() {
        this._characterQueue = new ArrayDeque<CharacterMatrix>();
    }

    public Deque<CharacterMatrix> getCharacterQueue() {
        return _characterQueue;
    }

    public void setCharacterQueue(Deque<CharacterMatrix> characterQueue) {
        this._characterQueue = characterQueue;
    }

    public boolean hasMore() {
       return !(_characterQueue.isEmpty() && (_ascmsg.length() == 0));
    }
}

