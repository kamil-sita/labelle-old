package place.sita.labelle;

public interface Copiable<T extends Copiable<T>> {

    T copy();

}
