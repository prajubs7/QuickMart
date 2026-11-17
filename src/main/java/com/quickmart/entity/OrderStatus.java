package com.quickmart.entity;

// Stored as text (@Enumerated(STRING)). ORDINAL would store 0,1,2 and silently
// corrupt every row the day someone inserts a new value in the middle.
public enum OrderStatus {
    PLACED
}
