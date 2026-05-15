package com.taskmanagement.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
public class PagedResponse<T> {
    private final List<T> content;
    private final int currentPage;
    private final int totalPages;
    private final long totalElements;
    private final int pageSize;
    private final boolean first;
    private final boolean last;

    private PagedResponse(Page<T> page) {
        this.content = page.getContent();
        this.currentPage = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.pageSize = page.getSize();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
    
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(page);
    }
}
