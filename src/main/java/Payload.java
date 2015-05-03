import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by shawnritchie on 23/04/15.
 */
public class Payload implements Serializable {

    private static final long serialVersionUID = 7243577654689882803L;

    private String name;
    private Integer version;
    private Boolean serializable;

    public Payload() {
        this.setName(null);
        this.setVersion(null);
        this.setSerializable(null);
    }

    public Payload(String name, Integer version, Boolean serializable) {
        this.setName(name);
        this.setVersion(version);
        this.setSerializable(serializable);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean isSerializable() {
        return serializable;
    }

    public void setSerializable(Boolean serializable) {
        this.serializable = serializable;
    }

    private void validateState() {
    }

    @Override
    public String toString() {
        return
            "Name: " + this.getName() + ", " +
            "Version: " + this.getVersion() + ", " +
            "Serializable: " + this.serializable.toString();
    }

    /**
     * Always treat de-serialization as a full-blown constructor, by
     * validating the final state of the de-serialized object.
     */
    private void readObject(
            ObjectInputStream aInputStream
    ) throws ClassNotFoundException, IOException {
        //always perform the default de-serialization first
        aInputStream.defaultReadObject();

        //ensure that object state has not been corrupted or tampered with maliciously
        validateState();
    }

    /**
     * This is the default implementation of writeObject.
     * Customise if necessary.
     */
    private void writeObject(
            ObjectOutputStream aOutputStream
    ) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }

}
