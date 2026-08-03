package com.platform.user.service.model;

/** A single organization directory search hit - just enough to render a result row/card and link
 * to the organization's landing page. Deliberately not the same shape as OrganizationResult (the
 * admin-panel one): this is public to every authenticated user, so it exposes only what any
 * visitor is meant to see, no membership/approval-setting internals. */
public record OrganizationSearchResult(String id, String name, String coverImageUrl, String logoImageUrl, int memberCount) {
}
