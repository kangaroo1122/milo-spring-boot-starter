package com.kangaroohy.milo.exception;

/**
 * 类 IdentityNotFoundException 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2021/09/15 09:35
 */
public class IdentityNotFoundException extends RuntimeException{
    public IdentityNotFoundException(String message) {
        super(message);
    }
}
