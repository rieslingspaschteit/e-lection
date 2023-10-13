/**
 * The interface Serializable is implemented by classes that can be sent to the Backend.
 * Explicitly defining the serialize() method makes it possible to make small changes to the export format without changing the classes attributes
 */
interface Serializable {
  stringify: () => object
}

/**
 * An interface describing functionality for classes that belong to an immutable model class and are used for creating immutable instances of these model classes.
 * Classes that implement this Interface make it possible to build and manipulate the state of the object in a step-by-step process
 * The method {@link Mutable.create} uses the state of the concrete mutable instance and constructs a new immutable instance of its associated model class.
 * @typeParam T the immutable model class the Mutable is associated with
 * @version 1.0
 */
interface Mutable<T> {

  /**
     * Creates a new instance of its associated class.
     * If this Mutable has any nested Mutable attributes, this method gets called on them as well.
     * @returns a new instance whose values are initialized from the this concrete Mutable's current values
     * @throws ConfigError if the provided values are insufficient or violate constraints for the immutable class attributes.
     */
  create: () => T
}

export type{
  Serializable,
  Mutable
}
