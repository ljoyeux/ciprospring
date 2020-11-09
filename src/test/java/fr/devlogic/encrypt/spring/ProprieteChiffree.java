package fr.devlogic.encrypt.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EncryptedProperty(algo = Constantes.ALGO_CRYPTO, key = Constantes.CRYPTO_KEY)
public @interface ProprieteChiffree {
    String profiles() default "";
}
