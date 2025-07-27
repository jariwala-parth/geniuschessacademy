package com.pjariwala.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {
  private List<T> content;
  private PageInfoDTO pageInfo;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PageInfoDTO {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
  }
}
