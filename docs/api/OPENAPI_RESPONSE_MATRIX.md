# HotelOS OpenAPI Response Matrix

## Canonical Gateway API (Port 8090)

This matrix defines the authoritative contract for all 83 externally accessible HotelOS Gateway endpoints.
All client communication, frontends, and integrations must align with these HTTP methods, success DTOs, and exact business error codes.

---

### Compact Summary Table

| Method | Path | Success | 400 | 401 | 403 | 404 | 409 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | 200 `LoginResponse` | `VALIDATION_ERROR` | `INVALID_CREDENTIALS` \| `ACCOUNT_DISABLED` | — | — | — |
| `POST` | `/api/auth/refresh` | 200 `RefreshResponse` | `VALIDATION_ERROR` | `INVALID_TOKEN` \| `TOKEN_EXPIRED` \| `TOKEN_REVOKED` | — | — | — |
| `POST` | `/api/auth/logout` | 204 No Content | `VALIDATION_ERROR` | — | — | — | — |
| `POST` | `/api/auth/invitations/accept` | 201 `StaffUserSummaryResponse` | `VALIDATION_ERROR` \| `INVALID_OR_EXPIRED_INVITATION` \| `PASSWORD_TOO_WEAK` | — | — | `INVITATION_NOT_FOUND` | `INVITATION_ALREADY_ACCEPTED` \| `USER_ALREADY_EXISTS` |
| `GET` | `/api/admin/users` | 200 `StaffUserPageResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/admin/users/{id}` | 200 `StaffUserDetailResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `USER_NOT_FOUND` | — |
| `POST` | `/api/admin/users/invitations` | 201 `InvitationCreatedResponse` | `VALIDATION_ERROR` \| `ROLE_NOT_ASSIGNABLE` \| `CANNOT_INVITE_ROLE` | `UNAUTHORIZED` | `ACCESS_DENIED` | — | `USER_ALREADY_EXISTS` \| `INVITATION_ALREADY_ACTIVE` |
| `POST` | `/api/admin/users/invitations/{id}/resend` | 200 `InvitationCreatedResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `INVITATION_NOT_FOUND` | `INVITATION_NOT_ACTIVE` \| `INVITATION_ALREADY_ACCEPTED` |
| `POST` | `/api/admin/users/{id}/disable` | 200 `StaffUserSummaryResponse` | `CANNOT_DISABLE_SELF` | `UNAUTHORIZED` | `ACCESS_DENIED` | `USER_NOT_FOUND` | `ACCOUNT_ALREADY_DISABLED` \| `CANNOT_DISABLE_LAST_ADMIN` \| `CANNOT_DISABLE_LAST_OWNER` |
| `POST` | `/api/admin/users/{id}/enable` | 200 `StaffUserSummaryResponse` | `ACCOUNT_ALREADY_ACTIVE` | `UNAUTHORIZED` | `ACCESS_DENIED` | `USER_NOT_FOUND` | — |
| `PUT` | `/api/admin/users/{id}/roles` | 200 `StaffUserDetailResponse` | `VALIDATION_ERROR` \| `CANNOT_DEMOTE_SELF` \| `ROLE_NOT_ASSIGNABLE` | `UNAUTHORIZED` | `ACCESS_DENIED` | `USER_NOT_FOUND` | `CANNOT_REMOVE_LAST_ADMIN` \| `CANNOT_REMOVE_LAST_OWNER` |
| `POST` | `/api/admin/users/{id}/revoke-sessions` | 204 No Content | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `USER_NOT_FOUND` | — |
| `POST` | `/api/ws/tickets/dashboard` | 200 `WebSocketTicketResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/reception/rooms` | 200 `List<Room>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/reception/rooms/{roomNumber}` | 200 `Room` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `ROOM_NOT_FOUND` | — |
| `GET` | `/api/reception/rooms/available` | 200 `List<Room>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/reception/check-in` | 200 `CheckInResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `GUEST_NOT_FOUND` | `NO_INVENTORY` \| `NO_ROOM_READY` \| `CONCURRENT_STATE_CHANGE` |
| `POST` | `/api/reception/check-out/{roomNumber}` | 200 `CheckOutResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `NO_ACTIVE_STAY` | `PENDING_ROOM_SERVICE_ORDERS` \| `STAY_ALREADY_CHECKED_OUT` \| `FREEZE_CONFLICT` |
| `GET` | `/api/reception/guests` | 200 `List<GuestStay>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/reception/guests/by-room/{roomNumber}` | 200 `GuestStay` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `NO_ACTIVE_STAY` | — |
| `PATCH` | `/api/reception/guests/{guestId}/archive` | 200 `GuestStay` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `GUEST_NOT_FOUND` | — |
| `POST` | `/api/reception/bills/{roomNumber}/calculate` | 200 `BillCalculationResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `ROOM_NOT_FOUND` | — |
| `POST` | `/api/reception/guest-profiles` | 201 `GuestProfileResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | — | `GUEST_ALREADY_EXISTS` |
| `GET` | `/api/reception/guest-profiles` | 200 `PageResponse<GuestProfileResponse>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/reception/guest-profiles/{id}` | 200 `GuestProfileResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `GUEST_NOT_FOUND` | — |
| `PUT` | `/api/reception/guest-profiles/{id}` | 200 `GuestProfileResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `GUEST_NOT_FOUND` | `GUEST_ALREADY_EXISTS` |
| `POST` | `/api/reception/reservations` | 201 `ReservationResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `GUEST_NOT_FOUND` \| `ROOM_NOT_FOUND` | `NO_INVENTORY` \| `ROOM_TYPE_MISMATCH` \| `ROOM_OUT_OF_ORDER` \| `ROOM_ALREADY_ALLOCATED` \| `IDEMPOTENCY_CONFLICT` |
| `GET` | `/api/reception/reservations` | 200 `PageResponse<ReservationResponse>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/reception/reservations/availability` | 200 `AvailabilityResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/reception/reservations/{id}` | 200 `ReservationResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `RESERVATION_NOT_FOUND` | — |
| `PATCH` | `/api/reception/reservations/{id}` | 200 `ReservationResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `RESERVATION_NOT_FOUND` | `RESERVATION_NOT_MUTABLE` \| `NO_INVENTORY` \| `ROOM_ALREADY_ALLOCATED` \| `IDEMPOTENCY_CONFLICT` \| `CONCURRENT_STATE_CHANGE` |
| `POST` | `/api/reception/reservations/{id}/cancel` | 200 `ReservationResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `RESERVATION_NOT_FOUND` | `INVALID_RESERVATION_TRANSITION` \| `CONCURRENT_STATE_CHANGE` |
| `POST` | `/api/reception/reservations/{id}/no-show` | 200 `ReservationResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `RESERVATION_NOT_FOUND` | `INVALID_RESERVATION_TRANSITION` \| `NO_SHOW_TOO_EARLY` \| `CONCURRENT_STATE_CHANGE` |
| `POST` | `/api/reception/reservations/{id}/assign-room` | 200 `ReservationResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `RESERVATION_NOT_FOUND` \| `ROOM_NOT_FOUND` | `ROOM_TYPE_MISMATCH` \| `ROOM_OUT_OF_ORDER` \| `ROOM_ALREADY_ALLOCATED` \| `RESERVATION_NOT_MUTABLE` |
| `POST` | `/api/reception/reservations/{id}/check-in` | 201 `StayResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `RESERVATION_NOT_FOUND` | `INVALID_RESERVATION_TRANSITION` \| `EARLY_CHECKIN_NOT_ALLOWED` \| `PAST_DEPARTURE_CHECKIN` \| `ASSIGNED_ROOM_NOT_READY` \| `NO_ROOM_READY` \| `CONCURRENT_STATE_CHANGE` |
| `GET` | `/api/reception/stays` | 200 `PageResponse<StayResponse>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/reception/stays/{stayId}` | 200 `StayResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `STAY_NOT_FOUND` | — |
| `POST` | `/api/reception/stays/{stayId}/extend` | 200 `StayResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `STAY_NOT_FOUND` | `STAY_ALREADY_CHECKED_OUT` \| `ROOM_ALREADY_ALLOCATED` \| `NO_INVENTORY` \| `CONCURRENT_STATE_CHANGE` |
| `POST` | `/api/reception/stays/{stayId}/change-room` | 200 `StayResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `STAY_NOT_FOUND` \| `ROOM_NOT_FOUND` | `STAY_ALREADY_CHECKED_OUT` \| `ROOM_NOT_READY` \| `ROOM_ALREADY_ALLOCATED` \| `CONCURRENT_STATE_CHANGE` |
| `POST` | `/api/reception/stays/{stayId}/check-out` | 200 `CheckOutResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `STAY_NOT_FOUND` | `STAY_ALREADY_CHECKED_OUT` \| `PENDING_ROOM_SERVICE_ORDERS` \| `FREEZE_CONFLICT` |
| `GET` | `/api/reception/stays/{stayId}/folio` | 200 `FolioResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `FOLIO_NOT_FOUND` | — |
| `GET` | `/api/reception/folios/{folioId}` | 200 `FolioResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `FOLIO_NOT_FOUND` | — |
| `GET` | `/api/reception/folios/{folioId}/entries` | 200 `Page<FolioEntryResponse>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `FOLIO_NOT_FOUND` | — |
| `POST` | `/api/reception/folios/{folioId}/adjustments` | 200 `FolioEntryResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `FOLIO_NOT_FOUND` | `FOLIO_CLOSED` \| `IDEMPOTENCY_CONFLICT` |
| `GET` | `/api/housekeeping/queue` | 200 `List<CleaningTask>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/housekeeping/queue/{roomNumber}` | 200 `CleaningTask` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/housekeeping/rooms/{roomNumber}/start` | 200 `CleaningTask` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `TASK_NOT_FOUND` | — |
| `POST` | `/api/housekeeping/rooms/{roomNumber}/clean` | 200 `CleaningTask` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `TASK_NOT_FOUND` | `MAINTENANCE_CLEANUP_REQUIRED` \| `MAINTENANCE_CLEANUP_NOT_COVERED` |
| `POST` | `/api/housekeeping/rooms/{roomNumber}/report-defect` | 200 `MaintenanceIssue` | `VALIDATION_ERROR` \| `IDEMPOTENCY_KEY_REQUIRED` | `UNAUTHORIZED` | `ACCESS_DENIED` | — | `IDEMPOTENCY_CONFLICT` |
| `PATCH` | `/api/housekeeping/queue/{roomNumber}/cancel` | 200 `CleaningTask` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `TASK_NOT_FOUND` | — |
| `GET` | `/api/housekeeping/cleaners` | 200 `List<String>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/room-service/orders` | 200 `List<RoomOrder>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/room-service/orders/{orderId}` | 200 `RoomOrder` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `ORDER_NOT_FOUND` | — |
| `POST` | `/api/room-service/orders` | 200 `RoomOrder` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `NO_ACTIVE_STAY` | `STAY_ORDERING_FROZEN` \| `STAY_ORDERING_CLOSED` |
| `PATCH` | `/api/room-service/orders/{orderId}/next` | 200 `RoomOrder` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `ORDER_NOT_FOUND` | `STAY_ORDERING_FROZEN` \| `STAY_ORDERING_CLOSED` |
| `PATCH` | `/api/room-service/orders/{orderId}/cancel` | 200 `RoomOrder` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `ORDER_NOT_FOUND` | `STAY_ORDERING_FROZEN` \| `STAY_ORDERING_CLOSED` |
| `GET` | `/api/room-service/charges/{roomNumber}` | 200 `RoomServiceChargesResponse` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/maintenance/issues` | 200 `List<MaintenanceIssue>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/maintenance/issues/{issueId}` | 200 `MaintenanceIssue` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | `ISSUE_NOT_FOUND` | — |
| `POST` | `/api/maintenance/issues` | 200 `MaintenanceIssue` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/maintenance/queue` | 200 `List<MaintenanceIssue>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/maintenance/queue/process-next` | 200 `List<MaintenanceIssue>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `PATCH` | `/api/maintenance/issues/{issueId}/resolve` | 200 `MaintenanceIssue` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `ISSUE_NOT_FOUND` | — |
| `POST` | `/api/maintenance/issues/{issueId}/resolve` | 200 `MaintenanceIssue` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `ISSUE_NOT_FOUND` | — |
| `PATCH` | `/api/maintenance/issues/{issueId}/cancel` | 200 `MaintenanceIssue` | `VALIDATION_ERROR` | `UNAUTHORIZED` | `ACCESS_DENIED` | `ISSUE_NOT_FOUND` | — |
| `GET` | `/api/maintenance/technicians` | 200 `List<String>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/dashboard/snapshot` | 200 `DashboardSnapshotResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/dashboard/events` | 200 `List<EventFeedDto>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `DELETE` | `/api/dashboard/events` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/dashboard/bootstrap` | 200 `BootstrapResultDto` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `GET` | `/api/gateway/health` | 200 `GatewayHealthResponse` | — | — | — | — | — |
| `GET` | `/api/gateway/info` | 200 `Map<String, Object>` | — | — | — | — | — |
| `GET` | `/api/gateway/routes` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/reset` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/seed` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-01` | 200 `CheckInResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-02` | 200 `CheckOutResponse` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-03` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-04` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-05` | 200 `MaintenanceIssue` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-06` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-07` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |
| `POST` | `/api/demo/run/ts-08` | 200 `Map<String, Object>` | — | `UNAUTHORIZED` | `ACCESS_DENIED` | — | — |

---

## Detailed Endpoint Specifications

### 1. Authentication

#### `POST /api/auth/login`
- **Summary**: Staff login and token issuance
- **OperationId**: `login`
- **Tag**: `Authentication`
- **Security**: None (Public)
- **Request Body**: `LoginRequest`
  - `username` (string, required)
  - `password` (string, required)
- **Responses**:
  - `200`: `LoginResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `INVALID_CREDENTIALS` | `ACCOUNT_DISABLED`

#### `POST /api/auth/refresh`
- **Summary**: Refresh staff access token
- **OperationId**: `refreshToken`
- **Tag**: `Authentication`
- **Security**: None (Public)
- **Request Body**: `RefreshRequest`
  - `refreshToken` (string, required)
- **Responses**:
  - `200`: `RefreshResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `INVALID_TOKEN` | `TOKEN_EXPIRED` | `TOKEN_REVOKED`

#### `POST /api/auth/logout`
- **Summary**: Revoke refresh session
- **OperationId**: `logout`
- **Tag**: `Authentication`
- **Security**: None (Public)
- **Request Body**: `LogoutRequest`
  - `refreshToken` (string, required)
- **Responses**:
  - `204`: No Content
  - `400`: `VALIDATION_ERROR`

#### `POST /api/auth/invitations/accept`
- **Summary**: Accept staff invitation and activate account
- **OperationId**: `acceptStaffInvitation`
- **Tag**: `Authentication`
- **Security**: None (Public)
- **Request Body**: `AcceptStaffInvitationRequest`
  - `token` (string, required)
  - `password` (string, required)
- **Responses**:
  - `201`: `StaffUserSummaryResponse`
  - `400`: `VALIDATION_ERROR` | `INVALID_OR_EXPIRED_INVITATION` | `PASSWORD_TOO_WEAK`
  - `404`: `INVITATION_NOT_FOUND`
  - `409`: `INVITATION_ALREADY_ACCEPTED` | `USER_ALREADY_EXISTS`

---

### 2. Staff Administration

#### `GET /api/admin/users`
- **Summary**: List staff accounts
- **OperationId**: `listStaffUsers`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Query Parameters**:
  - `page` (integer, default: 0)
  - `size` (integer, default: 20)
  - `status` (string, optional: ACTIVE, DISABLED)
  - `role` (string, optional)
  - `search` (string, optional)
- **Responses**:
  - `200`: `StaffUserPageResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/admin/users/{id}`
- **Summary**: Get staff account detail
- **OperationId**: `getStaffUser`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `200`: `StaffUserDetailResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `USER_NOT_FOUND`

#### `POST /api/admin/users/invitations`
- **Summary**: Create staff invitation
- **OperationId**: `createStaffInvitation`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Request Body**: `CreateStaffInvitationRequest`
  - `email` (string, required)
  - `fullName` (string, required)
  - `roles` (array of string, required)
- **Responses**:
  - `201`: `InvitationCreatedResponse`
  - `400`: `VALIDATION_ERROR` | `ROLE_NOT_ASSIGNABLE` | `CANNOT_INVITE_ROLE`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `409`: `USER_ALREADY_EXISTS` | `INVITATION_ALREADY_ACTIVE`

#### `POST /api/admin/users/invitations/{id}/resend`
- **Summary**: Resend staff invitation
- **OperationId**: `resendStaffInvitation`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `200`: `InvitationCreatedResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `INVITATION_NOT_FOUND`
  - `409`: `INVITATION_NOT_ACTIVE` | `INVITATION_ALREADY_ACCEPTED`

#### `POST /api/admin/users/{id}/disable`
- **Summary**: Disable staff user
- **OperationId**: `disableStaffUser`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Path Parameters**:
  - `id` (UUID, required)
- **Request Body**: `DisableStaffUserRequest` (optional)
  - `reason` (string, optional)
- **Responses**:
  - `200`: `StaffUserSummaryResponse`
  - `400`: `CANNOT_DISABLE_SELF`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `USER_NOT_FOUND`
  - `409`: `ACCOUNT_ALREADY_DISABLED` | `CANNOT_DISABLE_LAST_ADMIN` | `CANNOT_DISABLE_LAST_OWNER`

#### `POST /api/admin/users/{id}/enable`
- **Summary**: Enable staff user
- **OperationId**: `enableStaffUser`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `200`: `StaffUserSummaryResponse`
  - `400`: `ACCOUNT_ALREADY_ACTIVE`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `USER_NOT_FOUND`

#### `PUT /api/admin/users/{id}/roles`
- **Summary**: Update staff roles
- **OperationId**: `updateStaffRoles`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Path Parameters**:
  - `id` (UUID, required)
- **Request Body**: `UpdateStaffRolesRequest`
  - `roles` (array of string, required)
- **Responses**:
  - `200`: `StaffUserDetailResponse`
  - `400`: `VALIDATION_ERROR` | `CANNOT_DEMOTE_SELF` | `ROLE_NOT_ASSIGNABLE`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `USER_NOT_FOUND`
  - `409`: `CANNOT_REMOVE_LAST_ADMIN` | `CANNOT_REMOVE_LAST_OWNER`

#### `POST /api/admin/users/{id}/revoke-sessions`
- **Summary**: Revoke staff sessions
- **OperationId**: `revokeStaffSessions`
- **Tag**: `Staff Administration`
- **Security**: `staffBearerAuth` (ADMIN, OWNER)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `204`: No Content
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `USER_NOT_FOUND`

---

### 3. Dashboard

#### `POST /api/ws/tickets/dashboard`
- **Summary**: Issue dashboard WebSocket ticket
- **OperationId**: `issueDashboardWsTicket`
- **Tag**: `Dashboard`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER)
- **Responses**:
  - `200`: `WebSocketTicketResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/dashboard/snapshot`
- **Summary**: Aggregate full operational snapshot
- **OperationId**: `getDashboardSnapshot`
- **Tag**: `Dashboard`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER)
- **Responses**:
  - `200`: `DashboardSnapshotResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/dashboard/events`
- **Summary**: Get dashboard event history
- **OperationId**: `listDashboardEvents`
- **Tag**: `Dashboard`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER)
- **Responses**:
  - `200`: `List<EventFeedDto>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `DELETE /api/dashboard/events`
- **Summary**: Clear dashboard event history
- **OperationId**: `clearDashboardEvents`
- **Tag**: `Dashboard`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/dashboard/bootstrap`
- **Summary**: Trigger dashboard read-model bootstrap
- **OperationId**: `bootstrapDashboard`
- **Tag**: `Dashboard`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `BootstrapResultDto`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

---

### 4. Reception — Rooms

#### `GET /api/reception/rooms`
- **Summary**: List all hotel rooms
- **OperationId**: `listRooms`
- **Tag**: `Reception — Rooms`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Responses**:
  - `200`: `List<Room>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/reception/rooms/{roomNumber}`
- **Summary**: Get room by room number
- **OperationId**: `getRoom`
- **Tag**: `Reception — Rooms`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `Room`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ROOM_NOT_FOUND`

#### `GET /api/reception/rooms/available`
- **Summary**: Get available sellable rooms
- **OperationId**: `listAvailableRooms`
- **Tag**: `Reception — Rooms`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Query Parameters**:
  - `roomType` (string, optional)
  - `floor` (integer, optional)
- **Responses**:
  - `200`: `List<Room>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

---

### 5. Reception — Stays

#### `POST /api/reception/check-in`
- **Summary**: Walk-in guest check-in
- **OperationId**: `checkInGuest`
- **Tag**: `Reception — Stays`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Request Body**: `CheckInRequest`
  - `guestName` (string, required)
  - `roomType` (string, required)
  - `nights` (integer, required)
  - `preferredFloor` (integer, optional)
  - `proximity` (string, optional)
  - `guestId` (UUID, optional)
- **Responses**:
  - `200`: `CheckInResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `GUEST_NOT_FOUND`
  - `409`: `NO_INVENTORY` | `NO_ROOM_READY` | `CONCURRENT_STATE_CHANGE`

#### `POST /api/reception/check-out/{roomNumber}`
- **Summary**: Check out guest by room number
- **OperationId**: `checkOutGuest`
- **Tag**: `Reception — Stays`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `CheckOutResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `NO_ACTIVE_STAY`
  - `409`: `PENDING_ROOM_SERVICE_ORDERS` | `STAY_ALREADY_CHECKED_OUT` | `FREEZE_CONFLICT`

#### `GET /api/reception/stays`
- **Summary**: List guest stays
- **OperationId**: `listStays`
- **Tag**: `Reception — Stays`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Query Parameters**:
  - `page` (integer, default: 0)
  - `size` (integer, default: 20)
  - `sort` (string, optional)
  - `activeOnly` (boolean, optional)
  - `guestId` (UUID, optional)
  - `roomNumber` (string, optional)
- **Responses**:
  - `200`: `PageResponse<StayResponse>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/reception/stays/{stayId}`
- **Summary**: Get stay details by ID
- **OperationId**: `getStay`
- **Tag**: `Reception — Stays`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `stayId` (UUID, required)
- **Responses**:
  - `200`: `StayResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `STAY_NOT_FOUND`

#### `POST /api/reception/stays/{stayId}/extend`
- **Summary**: Extend active stay departure date
- **OperationId**: `extendStay`
- **Tag**: `Reception — Stays`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `stayId` (UUID, required)
- **Request Body**: `ExtendStayRequest`
  - `newDepartureDate` (LocalDate, required)
- **Responses**:
  - `200`: `StayResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `STAY_NOT_FOUND`
  - `409`: `STAY_ALREADY_CHECKED_OUT` | `ROOM_ALREADY_ALLOCATED` | `NO_INVENTORY` | `CONCURRENT_STATE_CHANGE`

#### `POST /api/reception/stays/{stayId}/change-room`
- **Summary**: Change assigned room for stay
- **OperationId**: `changeStayRoom`
- **Tag**: `Reception — Stays`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `stayId` (UUID, required)
- **Request Body**: `ChangeRoomRequest`
  - `targetRoomNumber` (string, required)
  - `reason` (string, optional)
- **Responses**:
  - `200`: `StayResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `STAY_NOT_FOUND` | `ROOM_NOT_FOUND`
  - `409`: `STAY_ALREADY_CHECKED_OUT` | `ROOM_NOT_READY` | `ROOM_ALREADY_ALLOCATED` | `CONCURRENT_STATE_CHANGE`

#### `POST /api/reception/stays/{stayId}/check-out`
- **Summary**: Check out stay by ID
- **OperationId**: `checkOutStay`
- **Tag**: `Reception — Stays`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `stayId` (UUID, required)
- **Responses**:
  - `200`: `CheckOutResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `STAY_NOT_FOUND`
  - `409`: `STAY_ALREADY_CHECKED_OUT` | `PENDING_ROOM_SERVICE_ORDERS` | `FREEZE_CONFLICT`

---

### 6. Reception — Guests

#### `GET /api/reception/guests`
- **Summary**: List active hotel guests
- **OperationId**: `listGuests`
- **Tag**: `Reception — Guests`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Responses**:
  - `200`: `List<GuestStay>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/reception/guests/by-room/{roomNumber}`
- **Summary**: Get current guest for room
- **OperationId**: `getGuestByRoom`
- **Tag**: `Reception — Guests`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `GuestStay`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `NO_ACTIVE_STAY`

#### `PATCH /api/reception/guests/{guestId}/archive`
- **Summary**: Archive guest record
- **OperationId**: `archiveGuest`
- **Tag**: `Reception — Guests`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `guestId` (string, required)
- **Responses**:
  - `200`: `GuestStay`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `GUEST_NOT_FOUND`

#### `POST /api/reception/guest-profiles`
- **Summary**: Create new guest profile
- **OperationId**: `createGuestProfile`
- **Tag**: `Reception — Guests`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Request Body**: `CreateGuestRequest`
  - `firstName` (string, required)
  - `lastName` (string, required)
  - `email` (string, optional)
  - `phone` (string, optional)
  - `identificationNumber` (string, optional)
- **Responses**:
  - `201`: `GuestProfileResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `409`: `GUEST_ALREADY_EXISTS`

#### `GET /api/reception/guest-profiles`
- **Summary**: List guest profiles with pagination
- **OperationId**: `listGuestProfiles`
- **Tag**: `Reception — Guests`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Query Parameters**:
  - `page` (integer, default: 0)
  - `size` (integer, default: 20)
  - `sort` (string, optional)
  - `search` (string, optional)
- **Responses**:
  - `200`: `PageResponse<GuestProfileResponse>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/reception/guest-profiles/{id}`
- **Summary**: Get guest profile by ID
- **OperationId**: `getGuestProfile`
- **Tag**: `Reception — Guests`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `200`: `GuestProfileResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `GUEST_NOT_FOUND`

#### `PUT /api/reception/guest-profiles/{id}`
- **Summary**: Update guest profile
- **OperationId**: `updateGuestProfile`
- **Tag**: `Reception — Guests`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Request Body**: `UpdateGuestRequest`
  - `firstName` (string, required)
  - `lastName` (string, required)
  - `email` (string, optional)
  - `phone` (string, optional)
  - `identificationNumber` (string, optional)
- **Responses**:
  - `200`: `GuestProfileResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `GUEST_NOT_FOUND`
  - `409`: `GUEST_ALREADY_EXISTS`

---

### 7. Reception — Reservations

#### `POST /api/reception/reservations`
- **Summary**: Create reservation
- **OperationId**: `createReservation`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Headers**:
  - `Idempotency-Key` (string, optional)
- **Request Body**: `CreateReservationRequest`
  - `guestId` (UUID, required)
  - `roomType` (string, required)
  - `arrivalDate` (LocalDate, required)
  - `departureDate` (LocalDate, required)
  - `nightlyRateSnapshot` (BigDecimal, required)
  - `currency` (string, required)
  - `assignedRoomNumber` (string, optional)
- **Responses**:
  - `201`: `ReservationResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `GUEST_NOT_FOUND` | `ROOM_NOT_FOUND`
  - `409`: `NO_INVENTORY` | `ROOM_TYPE_MISMATCH` | `ROOM_OUT_OF_ORDER` | `ROOM_ALREADY_ALLOCATED` | `IDEMPOTENCY_CONFLICT`

#### `GET /api/reception/reservations`
- **Summary**: List reservations
- **OperationId**: `listReservations`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Query Parameters**:
  - `page` (integer, default: 0)
  - `size` (integer, default: 20)
  - `sort` (string, optional)
  - `status` (string, optional)
  - `guestId` (UUID, optional)
  - `roomType` (string, optional)
  - `arrivalFrom` (LocalDate, optional)
  - `arrivalTo` (LocalDate, optional)
  - `departureFrom` (LocalDate, optional)
  - `departureTo` (LocalDate, optional)
  - `assignedRoomNumber` (string, optional)
- **Responses**:
  - `200`: `PageResponse<ReservationResponse>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/reception/reservations/availability`
- **Summary**: Query room type availability
- **OperationId**: `checkReservationAvailability`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Query Parameters**:
  - `roomType` (string, required)
  - `arrivalDate` (LocalDate, required)
  - `departureDate` (LocalDate, required)
- **Responses**:
  - `200`: `AvailabilityResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/reception/reservations/{id}`
- **Summary**: Get reservation by ID
- **OperationId**: `getReservation`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `200`: `ReservationResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `RESERVATION_NOT_FOUND`

#### `PATCH /api/reception/reservations/{id}`
- **Summary**: Modify confirmed reservation
- **OperationId**: `patchReservation`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Request Body**: `PatchReservationRequest`
- **Responses**:
  - `200`: `ReservationResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `RESERVATION_NOT_FOUND`
  - `409`: `RESERVATION_NOT_MUTABLE` | `NO_INVENTORY` | `ROOM_ALREADY_ALLOCATED` | `IDEMPOTENCY_CONFLICT` | `CONCURRENT_STATE_CHANGE`

#### `POST /api/reception/reservations/{id}/cancel`
- **Summary**: Cancel confirmed reservation
- **OperationId**: `cancelReservation`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `200`: `ReservationResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `RESERVATION_NOT_FOUND`
  - `409`: `INVALID_RESERVATION_TRANSITION` | `CONCURRENT_STATE_CHANGE`

#### `POST /api/reception/reservations/{id}/no-show`
- **Summary**: Mark confirmed reservation as no-show
- **OperationId**: `noShowReservation`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `200`: `ReservationResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `RESERVATION_NOT_FOUND`
  - `409`: `INVALID_RESERVATION_TRANSITION` | `NO_SHOW_TOO_EARLY` | `CONCURRENT_STATE_CHANGE`

#### `POST /api/reception/reservations/{id}/assign-room`
- **Summary**: Pre-assign room to reservation
- **OperationId**: `assignReservationRoom`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Request Body**: `AssignRoomRequest`
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `ReservationResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `RESERVATION_NOT_FOUND` | `ROOM_NOT_FOUND`
  - `409`: `ROOM_TYPE_MISMATCH` | `ROOM_OUT_OF_ORDER` | `ROOM_ALREADY_ALLOCATED` | `RESERVATION_NOT_MUTABLE`

#### `POST /api/reception/reservations/{id}/check-in`
- **Summary**: Check in reservation
- **OperationId**: `checkInReservation`
- **Tag**: `Reception — Reservations`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `id` (UUID, required)
- **Responses**:
  - `201`: `StayResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `RESERVATION_NOT_FOUND`
  - `409`: `INVALID_RESERVATION_TRANSITION` | `EARLY_CHECKIN_NOT_ALLOWED` | `PAST_DEPARTURE_CHECKIN` | `ASSIGNED_ROOM_NOT_READY` | `NO_ROOM_READY` | `CONCURRENT_STATE_CHANGE`

---

### 8. Reception — Folio

#### `POST /api/reception/bills/{roomNumber}/calculate`
- **Summary**: Calculate current room bill
- **OperationId**: `calculateRoomBill`
- **Tag**: `Reception — Folio`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `BillCalculationResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ROOM_NOT_FOUND`

#### `GET /api/reception/stays/{stayId}/folio`
- **Summary**: Get guest folio by stay ID
- **OperationId**: `getFolioByStay`
- **Tag**: `Reception — Folio`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `stayId` (UUID, required)
- **Responses**:
  - `200`: `FolioResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `FOLIO_NOT_FOUND`

#### `GET /api/reception/folios/{folioId}`
- **Summary**: Get folio by folio ID
- **OperationId**: `getFolio`
- **Tag**: `Reception — Folio`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `folioId` (UUID, required)
- **Responses**:
  - `200`: `FolioResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `FOLIO_NOT_FOUND`

#### `GET /api/reception/folios/{folioId}/entries`
- **Summary**: List folio entries
- **OperationId**: `listFolioEntries`
- **Tag**: `Reception — Folio`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `folioId` (UUID, required)
- **Query Parameters**:
  - `entryType` (string, optional: CHARGE, PAYMENT, ADJUSTMENT)
  - `page` (integer, default: 0)
  - `size` (integer, default: 20)
  - `sort` (string, default: "occurredAt,asc")
- **Responses**:
  - `200`: `Page<FolioEntryResponse>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `FOLIO_NOT_FOUND`

#### `POST /api/reception/folios/{folioId}/adjustments`
- **Summary**: Post manual folio adjustment
- **OperationId**: `createFolioAdjustment`
- **Tag**: `Reception — Folio`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, RECEPTIONIST)
- **Path Parameters**:
  - `folioId` (UUID, required)
- **Headers**:
  - `Idempotency-Key` (string, optional)
- **Request Body**: `CreateAdjustmentRequest`
  - `entryType` (string, required: CHARGE, PAYMENT, ADJUSTMENT)
  - `amount` (BigDecimal, required)
  - `description` (string, required)
- **Responses**:
  - `200`: `FolioEntryResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `FOLIO_NOT_FOUND`
  - `409`: `FOLIO_CLOSED` | `IDEMPOTENCY_CONFLICT`

---

### 9. Housekeeping

#### `GET /api/housekeeping/queue`
- **Summary**: Get housekeeping queue
- **OperationId**: `getHousekeepingQueue`
- **Tag**: `Housekeeping`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, HOUSEKEEPING)
- **Responses**:
  - `200`: `List<CleaningTask>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/housekeeping/queue/{roomNumber}`
- **Summary**: Add room to housekeeping queue
- **OperationId**: `enqueueHousekeepingTask`
- **Tag**: `Housekeeping`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, HOUSEKEEPING)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `CleaningTask`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/housekeeping/rooms/{roomNumber}/start`
- **Summary**: Start cleaning room
- **OperationId**: `startHousekeepingCleaning`
- **Tag**: `Housekeeping`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, HOUSEKEEPING)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `CleaningTask`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `TASK_NOT_FOUND`

#### `POST /api/housekeeping/rooms/{roomNumber}/clean`
- **Summary**: Mark room clean
- **OperationId**: `completeHousekeepingCleaning`
- **Tag**: `Housekeeping`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, HOUSEKEEPING)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `CleaningTask`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `TASK_NOT_FOUND`
  - `409`: `MAINTENANCE_CLEANUP_REQUIRED` | `MAINTENANCE_CLEANUP_NOT_COVERED`

#### `POST /api/housekeeping/rooms/{roomNumber}/report-defect`
- **Summary**: Report room defect from housekeeping
- **OperationId**: `reportHousekeepingDefect`
- **Tag**: `Housekeeping`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, HOUSEKEEPING)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Headers**:
  - `Idempotency-Key` (string, required)
- **Request Body**: `ReportDefectRequest`
  - `description` (string, required)
  - `priority` (string, required: CRITICAL, HIGH, NORMAL, LOW)
- **Responses**:
  - `200`: `MaintenanceIssue`
  - `400`: `VALIDATION_ERROR` | `IDEMPOTENCY_KEY_REQUIRED`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `409`: `IDEMPOTENCY_CONFLICT`

#### `PATCH /api/housekeeping/queue/{roomNumber}/cancel`
- **Summary**: Cancel housekeeping queue item
- **OperationId**: `cancelHousekeepingCleaning`
- **Tag**: `Housekeeping`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, HOUSEKEEPING)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `CleaningTask`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `TASK_NOT_FOUND`

#### `GET /api/housekeeping/cleaners`
- **Summary**: Get housekeeping cleaner staff list
- **OperationId**: `listHousekeepingCleaners`
- **Tag**: `Housekeeping`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, HOUSEKEEPING)
- **Responses**:
  - `200`: `List<String>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

---

### 10. Room Service

#### `GET /api/room-service/orders`
- **Summary**: Get room service orders
- **OperationId**: `listRoomServiceOrders`
- **Tag**: `Room Service`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, ROOM_SERVICE)
- **Responses**:
  - `200`: `List<RoomOrder>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/room-service/orders/{orderId}`
- **Summary**: Get room service order by ID
- **OperationId**: `getRoomServiceOrder`
- **Tag**: `Room Service`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, ROOM_SERVICE)
- **Path Parameters**:
  - `orderId` (string, required)
- **Responses**:
  - `200`: `RoomOrder`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ORDER_NOT_FOUND`

#### `POST /api/room-service/orders`
- **Summary**: Create room service order
- **OperationId**: `createRoomServiceOrder`
- **Tag**: `Room Service`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, ROOM_SERVICE)
- **Request Body**: `CreateOrderRequest`
  - `roomNumber` (string, required)
  - `currency` (string, optional: default "UZS")
  - `items` (array of `OrderItemRequest`, required)
- **Responses**:
  - `200`: `RoomOrder`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `NO_ACTIVE_STAY`
  - `409`: `STAY_ORDERING_FROZEN` | `STAY_ORDERING_CLOSED`

#### `PATCH /api/room-service/orders/{orderId}/next`
- **Summary**: Advance room service order status
- **OperationId**: `advanceRoomServiceOrder`
- **Tag**: `Room Service`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, ROOM_SERVICE)
- **Path Parameters**:
  - `orderId` (string, required)
- **Responses**:
  - `200`: `RoomOrder`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ORDER_NOT_FOUND`
  - `409`: `STAY_ORDERING_FROZEN` | `STAY_ORDERING_CLOSED`

#### `PATCH /api/room-service/orders/{orderId}/cancel`
- **Summary**: Cancel room service order
- **OperationId**: `cancelRoomServiceOrder`
- **Tag**: `Room Service`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, ROOM_SERVICE)
- **Path Parameters**:
  - `orderId` (string, required)
- **Responses**:
  - `200`: `RoomOrder`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ORDER_NOT_FOUND`
  - `409`: `STAY_ORDERING_FROZEN` | `STAY_ORDERING_CLOSED`

#### `GET /api/room-service/charges/{roomNumber}`
- **Summary**: Get room service charges by room
- **OperationId**: `getRoomServiceCharges`
- **Tag**: `Room Service`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, ROOM_SERVICE)
- **Path Parameters**:
  - `roomNumber` (string, required)
- **Responses**:
  - `200`: `RoomServiceChargesResponse`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

---

### 11. Maintenance

#### `GET /api/maintenance/issues`
- **Summary**: Get maintenance issues
- **OperationId**: `listMaintenanceIssues`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Responses**:
  - `200`: `List<MaintenanceIssue>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/maintenance/issues/{issueId}`
- **Summary**: Get maintenance issue by ID
- **OperationId**: `getMaintenanceIssue`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Path Parameters**:
  - `issueId` (string, required)
- **Responses**:
  - `200`: `MaintenanceIssue`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ISSUE_NOT_FOUND`

#### `POST /api/maintenance/issues`
- **Summary**: Report maintenance issue
- **OperationId**: `createMaintenanceIssue`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Request Body**: `CreateIssueRequest`
  - `roomNumber` (string, required)
  - `description` (string, required)
  - `priority` (string, required: CRITICAL, HIGH, NORMAL, LOW)
- **Responses**:
  - `200`: `MaintenanceIssue`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `GET /api/maintenance/queue`
- **Summary**: Get maintenance priority queue
- **OperationId**: `getMaintenanceQueue`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Responses**:
  - `200`: `List<MaintenanceIssue>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/maintenance/queue/process-next`
- **Summary**: Process next maintenance queue item
- **OperationId**: `processNextMaintenanceQueueItem`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Responses**:
  - `200`: `List<MaintenanceIssue>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `PATCH /api/maintenance/issues/{issueId}/resolve`
- **Summary**: Resolve maintenance issue
- **OperationId**: `resolveMaintenanceIssue`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Path Parameters**:
  - `issueId` (string, required)
- **Request Body**: `ResolveIssueRequest` (optional)
  - `requiresCleaning` (boolean, optional)
- **Responses**:
  - `200`: `MaintenanceIssue`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ISSUE_NOT_FOUND`

#### `POST /api/maintenance/issues/{issueId}/resolve`
- **Summary**: Resolve maintenance issue (POST alias)
- **OperationId**: `resolveMaintenanceIssuePost`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Path Parameters**:
  - `issueId` (string, required)
- **Request Body**: `ResolveIssueRequest` (optional)
  - `requiresCleaning` (boolean, optional)
- **Responses**:
  - `200`: `MaintenanceIssue`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ISSUE_NOT_FOUND`

#### `PATCH /api/maintenance/issues/{issueId}/cancel`
- **Summary**: Cancel maintenance issue
- **OperationId**: `cancelMaintenanceIssue`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Path Parameters**:
  - `issueId` (string, required)
- **Responses**:
  - `200`: `MaintenanceIssue`
  - `400`: `VALIDATION_ERROR`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
  - `404`: `ISSUE_NOT_FOUND`

#### `GET /api/maintenance/technicians`
- **Summary**: Get maintenance technicians list
- **OperationId**: `listMaintenanceTechnicians`
- **Tag**: `Maintenance`
- **Security**: `staffBearerAuth` (ADMIN, MANAGER, MAINTENANCE)
- **Responses**:
  - `200`: `List<String>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

---

### 12. System

#### `GET /api/gateway/health`
- **Summary**: Gateway health check
- **OperationId**: `getGatewayHealth`
- **Tag**: `System`
- **Security**: None (Public)
- **Responses**:
  - `200`: `GatewayHealthResponse`

#### `GET /api/gateway/info`
- **Summary**: Gateway system information
- **OperationId**: `getGatewayInfo`
- **Tag**: `System`
- **Security**: None (Public)
- **Responses**:
  - `200`: `Map<String, Object>`

#### `GET /api/gateway/routes`
- **Summary**: List gateway routes
- **OperationId**: `listGatewayRoutes`
- **Tag**: `System`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

---

### 13. Development / Demo

> [!IMPORTANT]
> Development/demo workflows. Available only through development Gateway. Blocked by production edge. Not part of production client API. Roles: ADMIN.

#### `POST /api/demo/reset`
- **Summary**: Reset hotel demonstration state
- **OperationId**: `resetDemoData`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/seed`
- **Summary**: Seed hotel demonstration data
- **OperationId**: `seedDemoData`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-01`
- **Summary**: Run scenario TS-01 (Check-in)
- **OperationId**: `runDemoTs01`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `CheckInResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-02`
- **Summary**: Run scenario TS-02 (Check-out)
- **OperationId**: `runDemoTs02`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `CheckOutResponse`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-03`
- **Summary**: Run scenario TS-03 (Housekeeping flow)
- **OperationId**: `runDemoTs03`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-04`
- **Summary**: Run scenario TS-04 (Room service lifecycle)
- **OperationId**: `runDemoTs04`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-05`
- **Summary**: Run scenario TS-05 (Maintenance report)
- **OperationId**: `runDemoTs05`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `MaintenanceIssue`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-06`
- **Summary**: Run scenario TS-06 (Concurrent check-in)
- **OperationId**: `runDemoTs06`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-07`
- **Summary**: Run scenario TS-07 (Overbooking validation)
- **OperationId**: `runDemoTs07`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`

#### `POST /api/demo/run/ts-08`
- **Summary**: Run scenario TS-08 (Checkout invalid room)
- **OperationId**: `runDemoTs08`
- **Tag**: `Development / Demo`
- **Security**: `staffBearerAuth` (ADMIN)
- **Responses**:
  - `200`: `Map<String, Object>`
  - `401`: `UNAUTHORIZED`
  - `403`: `ACCESS_DENIED`
