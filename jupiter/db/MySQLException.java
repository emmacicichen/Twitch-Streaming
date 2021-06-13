package com.laioffer.jupiter.db;

public class MySQLException extends RuntimeException {
    public MySQLException(String errorMessage) {
        super(errorMessage);//call its parent's constructor
    }
}
//add or delete fav , we combine all the exceptions into a class, to make servlet handle easily
