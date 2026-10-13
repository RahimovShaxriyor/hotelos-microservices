package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class StaffUserPageResponse {

    @Schema(description = "Paged list of staff user summaries")
    private List<StaffUserSummaryResponse> items;

    @Schema(description = "Total count of matching records")
    private long total;

    @Schema(description = "Current page index (0-based)")
    private int page;

    @Schema(description = "Page size")
    private int size;

    @Schema(description = "Total number of pages")
    private int totalPages;

    public StaffUserPageResponse() {
    }

    public StaffUserPageResponse(List<StaffUserSummaryResponse> items, long total, int page, int size, int totalPages) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }

    public List<StaffUserSummaryResponse> getItems() {
        return items;
    }

    public void setItems(List<StaffUserSummaryResponse> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
