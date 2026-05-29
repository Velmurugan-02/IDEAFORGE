package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

/** Returned by GET /api/ideas/{id}/view-stats */
@Data @Builder
public class ViewStatsResponse {
    private int totalViews;
    private int uniqueViewers;
    private int viewsToday;
    private int viewsThisWeek;
    private int consentedViewers;
}
