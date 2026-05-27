package com.example.petshopapi.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String mensagem) { super(mensagem); }
}
