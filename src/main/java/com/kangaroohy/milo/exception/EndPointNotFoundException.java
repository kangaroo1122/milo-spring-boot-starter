package com.kangaroohy.milo.exception;

/**
 * 类 EndPointNotFoundException 功能描述：
 *
 * @author kangaroo hy
 * @version 0.0.1
 * @date 2021/09/04 17:03
 */
public class EndPointNotFoundException extends RuntimeException{
    public EndPointNotFoundException() {
        super();
    }

    public EndPointNotFoundException(String message) {
        super(message);
    }
}
