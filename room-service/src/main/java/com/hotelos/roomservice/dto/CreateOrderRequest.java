package com.hotelos.roomservice.dto;

import com.hotelos.roomservice.domain.OrderItem;
import java.util.List;

public class CreateOrderRequest {
    private String roomNumber;
    private List<OrderItem> items;
    private String currency = "UZS";

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
