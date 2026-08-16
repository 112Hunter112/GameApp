package com.parth.sportsapp.sportsbackend.dto;

import java.util.List;

/** One page of the social feed, cache-friendly (plain data, JSON-stable). */
public class FeedPageResponse {

  private List<FeedItemResponse> items;
  private int page;
  private boolean hasMore;

  public FeedPageResponse() {}

  public FeedPageResponse(List<FeedItemResponse> items, int page, boolean hasMore) {
    this.items = items;
    this.page = page;
    this.hasMore = hasMore;
  }

  public List<FeedItemResponse> getItems() { return items; }
  public void setItems(List<FeedItemResponse> items) { this.items = items; }

  public int getPage() { return page; }
  public void setPage(int page) { this.page = page; }

  public boolean isHasMore() { return hasMore; }
  public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}
