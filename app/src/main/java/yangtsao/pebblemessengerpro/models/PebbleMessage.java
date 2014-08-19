package yangtsao.pebblemessengerpro.models;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by yunshansimon on 14-8-19.
 */
public class PebbleMessage implements Serializable {


    private static final long serialVersionUID = -64368242386238449L;
    private Deque<CharacterMatrix> _characterQueue;
    /* store the unicode character's imgs and their positions */
    private String                 _ascmsg;

    /*
     * store the ascii String and blank space where should display unicode
     * character
     */

    public String getAscMsg() {
        return _ascmsg;
    }

    public void setAscMsg(String msg) {
        this._ascmsg = msg;
    }

    public void AddCharToAscMsg(char c) {
        this._ascmsg += String.valueOf(c);

    }

    public void AddStringToAscMsg(String s) {
        this._ascmsg += s;
    }

    public PebbleMessage(Deque<CharacterMatrix> characterQueue) {
        this._characterQueue = characterQueue;
    }

    public PebbleMessage() {
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
