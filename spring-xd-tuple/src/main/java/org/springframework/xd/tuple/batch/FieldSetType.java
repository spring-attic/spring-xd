package org.springframework.xd.tuple.batch;

/**
 * Enum used to provide typing information when reading
 * from a file.
 * 
 * @author Michael Minella
 * @see TupleFieldSetMapper
 */
public enum FieldSetType {
	STRING, CHAR, BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BIG_DECIMAL, DATE;
}
