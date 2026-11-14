# API route catalog

Controller mappings from application commit `94de3a0`. This lists declared routes. Check each controller, DTO, security configuration and service for supported parameters, permissions and response fields. Actuator and Spring Security framework routes are described in the [technical handbook](../TECHNICAL_HANDBOOK.md).

Staging clients use the staging origin. Paths below include their API prefixes. The [earlier API design](OPENAPI_SPECIFICATION.md) describes proposed interfaces.

## auth

| Method | Path | Controller |
| --- | --- | --- |
| GET | `/api/admin/stats` | [AdminController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AdminController.java) |
| GET | `/api/admin/kpis` | [AdminController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AdminController.java) |
| GET | `/api/admin/verifications` | [AdminController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AdminController.java) |
| PUT | `/api/admin/verifications/{vetUserId}/status` | [AdminController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AdminController.java) |
| GET | `/api/admin/users` | [AdminController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AdminController.java) |
| PATCH | `/api/admin/users/{userId}/status` | [AdminController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AdminController.java) |
| GET | `/api/admin/telemetry` | [AdminController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AdminController.java) |
| POST | `/api/auth/refresh` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/exchange` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/google` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/apple` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/logout` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| DELETE | `/api/auth/account` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| GET | `/api/auth/health` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/demo-login` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/login` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/register` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/otp/send` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/otp/verify` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| POST | `/api/auth/login-otp` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| GET | `/api/auth/me` | [AuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/AuthController.java) |
| GET | `/owner/login` | [StagingSpaController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/StagingSpaController.java) |
| GET | `/owner/register` | [StagingSpaController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/StagingSpaController.java) |
| GET | `/vet/login` | [StagingSpaController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/StagingSpaController.java) |
| GET | `/vet/register` | [StagingSpaController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/StagingSpaController.java) |
| GET | `/login` | [StagingSpaController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/StagingSpaController.java) |
| GET | `/` | [SystemController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/SystemController.java) |
| GET | `/api/public/health` | [SystemController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/SystemController.java) |
| GET | `/api/public/db-inspect` | [SystemController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/SystemController.java) |
| GET | `/api/users/profile` | [UserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/UserController.java) |
| GET | `/api/users/vet-portal` | [UserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/UserController.java) |
| GET | `/api/users/admin-portal` | [UserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/UserController.java) |
| GET | `/api/public/whatsapp/webhook` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |
| GET | `/api/public/whatsapp/webhook/` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |
| GET | `/api/auth/whatsapp/webhook` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |
| GET | `/api/auth/whatsapp/webhook/` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |
| POST | `/api/public/whatsapp/webhook` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |
| POST | `/api/public/whatsapp/webhook/` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |
| POST | `/api/auth/whatsapp/webhook` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |
| POST | `/api/auth/whatsapp/webhook/` | [WhatsAppWebhookController](../../Vetalya_backend/src/main/java/com/pet/vetalya/auth/controller/WhatsAppWebhookController.java) |

## clinic

| Method | Path | Controller |
| --- | --- | --- |
| GET | `/api/clinic/hospitals` | [ClinicWorkspaceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/clinic/ClinicWorkspaceController.java) |
| GET | `/api/clinic/hospitals/{hospitalId}/dashboard` | [ClinicWorkspaceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/clinic/ClinicWorkspaceController.java) |
| GET | `/api/clinic/hospitals/{hospitalId}/staff` | [ClinicWorkspaceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/clinic/ClinicWorkspaceController.java) |
| PUT | `/api/clinic/hospitals/{hospitalId}/staff` | [ClinicWorkspaceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/clinic/ClinicWorkspaceController.java) |
| POST | `/api/clinic/hospitals/{hospitalId}/tasks` | [ClinicWorkspaceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/clinic/ClinicWorkspaceController.java) |
| POST | `/api/clinic/hospitals/{hospitalId}/tasks/{taskId}/complete` | [ClinicWorkspaceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/clinic/ClinicWorkspaceController.java) |
| POST | `/api/clinic/hospitals/{hospitalId}/appointments/{appointmentId}/check-in` | [ClinicWorkspaceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/clinic/ClinicWorkspaceController.java) |

## controller

| Method | Path | Controller |
| --- | --- | --- |
| POST | `/api/v1/slots/generate` | [AppointmentSlotController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/AppointmentSlotController.java) |
| GET | `/api/v1/slots/available` | [AppointmentSlotController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/AppointmentSlotController.java) |
| POST | `/api/v1/slots/book` | [AppointmentSlotController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/AppointmentSlotController.java) |
| POST | `/api/v1/slots/appointments/{appointmentId}/check-in` | [AppointmentSlotController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/AppointmentSlotController.java) |
| POST | `/api/v1/slots/appointments/{appointmentId}/start-consultation` | [AppointmentSlotController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/AppointmentSlotController.java) |
| POST | `/api/v1/slots/appointments/{appointmentId}/end-consultation` | [AppointmentSlotController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/AppointmentSlotController.java) |
| POST | `/api/v1/encounters` | [ClinicalEncounterController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/ClinicalEncounterController.java) |
| GET | `/api/v1/encounters/{encounterId}` | [ClinicalEncounterController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/ClinicalEncounterController.java) |
| PUT | `/api/v1/encounters/{encounterId}` | [ClinicalEncounterController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/ClinicalEncounterController.java) |
| POST | `/api/v1/encounters/{encounterId}/sign` | [ClinicalEncounterController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/ClinicalEncounterController.java) |
| POST | `/api/v1/encounters/{encounterId}/amend` | [ClinicalEncounterController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/ClinicalEncounterController.java) |
| GET | `/api/v1/encounters/pet/{petId}` | [ClinicalEncounterController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/ClinicalEncounterController.java) |
| POST | `/api/v1/diagnostics/orders` | [DiagnosticController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/DiagnosticController.java) |
| GET | `/api/v1/diagnostics/orders/{orderId}` | [DiagnosticController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/DiagnosticController.java) |
| GET | `/api/v1/diagnostics/pet/{petId}` | [DiagnosticController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/DiagnosticController.java) |
| PUT | `/api/v1/diagnostics/items/{itemId}/results` | [DiagnosticController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/DiagnosticController.java) |
| GET | `/api/v1/diagnostics/catalog` | [DiagnosticController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/DiagnosticController.java) |
| GET | `/api/v1/pets/{petId}/passport` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| PUT | `/api/v1/pets/{petId}/passport` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| GET | `/api/v1/pets/{petId}/passport/timeline` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| GET | `/api/v1/pets/emergency/{qrHash}` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| GET | `/api/v1/pets/{petId}/passport/share-requests` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| POST | `/api/v1/pets/{petId}/passport/share-requests` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| POST | `/api/v1/pets/share-requests/{requestId}/approve` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| POST | `/api/v1/pets/share-requests/{requestId}/revoke` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| POST | `/api/v1/pets/{petId}/documents` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |
| GET | `/api/v1/pets/{petId}/documents` | [PetPassportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/controller/PetPassportController.java) |

## medshop

| Method | Path | Controller |
| --- | --- | --- |
| GET | `/api/store/audit-logs` | [StoreAuditController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/audit/controller/StoreAuditController.java) |
| POST | `/api/store/auth/login` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/register-shop` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/register-shop/social` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/refresh` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/logout` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/logout-all` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| GET | `/api/store/auth/me` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/change-password` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/exchange` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/google` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/apple` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/auth/link/{provider}` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| DELETE | `/api/store/auth/link/{provider}` | [StoreAuthController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/auth/controller/StoreAuthController.java) |
| POST | `/api/store/invoices` | [InvoiceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/billing/controller/InvoiceController.java) |
| GET | `/api/store/invoices/{id}` | [InvoiceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/billing/controller/InvoiceController.java) |
| GET | `/api/store/invoices` | [InvoiceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/billing/controller/InvoiceController.java) |
| POST | `/api/store/invoices/{id}/cancel` | [InvoiceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/billing/controller/InvoiceController.java) |
| GET | `/api/store/invoices/{id}/pdf` | [InvoiceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/billing/controller/InvoiceController.java) |
| GET | `/api/store/schedule-h-register` | [InvoiceController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/billing/controller/InvoiceController.java) |
| POST | `/api/store/categories` | [CategoryController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/CategoryController.java) |
| GET | `/api/store/categories` | [CategoryController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/CategoryController.java) |
| GET | `/api/store/categories/{id}` | [CategoryController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/CategoryController.java) |
| PUT | `/api/store/categories/{id}` | [CategoryController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/CategoryController.java) |
| DELETE | `/api/store/categories/{id}` | [CategoryController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/CategoryController.java) |
| POST | `/api/store/medicines` | [MedicineController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/MedicineController.java) |
| GET | `/api/store/medicines` | [MedicineController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/MedicineController.java) |
| GET | `/api/store/medicines/{id}` | [MedicineController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/MedicineController.java) |
| PUT | `/api/store/medicines/{id}` | [MedicineController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/MedicineController.java) |
| DELETE | `/api/store/medicines/{id}` | [MedicineController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/MedicineController.java) |
| GET | `/api/store/medicines/low-stock` | [MedicineController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/MedicineController.java) |
| POST | `/api/store/suppliers` | [SupplierController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/SupplierController.java) |
| GET | `/api/store/suppliers` | [SupplierController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/SupplierController.java) |
| GET | `/api/store/suppliers/{id}` | [SupplierController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/SupplierController.java) |
| PUT | `/api/store/suppliers/{id}` | [SupplierController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/SupplierController.java) |
| DELETE | `/api/store/suppliers/{id}` | [SupplierController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/catalog/controller/SupplierController.java) |
| POST | `/api/store/customers` | [CustomerController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/customer/controller/CustomerController.java) |
| GET | `/api/store/customers` | [CustomerController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/customer/controller/CustomerController.java) |
| GET | `/api/store/customers/{id}` | [CustomerController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/customer/controller/CustomerController.java) |
| PUT | `/api/store/customers/{id}` | [CustomerController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/customer/controller/CustomerController.java) |
| DELETE | `/api/store/customers/{id}` | [CustomerController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/customer/controller/CustomerController.java) |
| GET | `/api/store/customers/{id}/invoices` | [CustomerController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/customer/controller/CustomerController.java) |
| POST | `/api/store/gst/returns/generate` | [GstReturnController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/gst/controller/GstReturnController.java) |
| GET | `/api/store/gst/returns` | [GstReturnController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/gst/controller/GstReturnController.java) |
| GET | `/api/store/gst/returns/{id}` | [GstReturnController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/gst/controller/GstReturnController.java) |
| POST | `/api/store/gst/returns/{id}/mark-filed` | [GstReturnController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/gst/controller/GstReturnController.java) |
| GET | `/api/store/gst/returns/{id}/export` | [GstReturnController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/gst/controller/GstReturnController.java) |
| GET | `/api/store/dashboard` | [ReportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/report/controller/ReportController.java) |
| GET | `/api/store/reports/sales` | [ReportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/report/controller/ReportController.java) |
| GET | `/api/store/reports/top-medicines` | [ReportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/report/controller/ReportController.java) |
| GET | `/api/store/reports/profit` | [ReportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/report/controller/ReportController.java) |
| GET | `/api/store/reports/expiry` | [ReportController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/report/controller/ReportController.java) |
| POST | `/api/store/stock/receive` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| GET | `/api/store/stock/batches` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| GET | `/api/store/stock/batches/{id}` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| PUT | `/api/store/stock/batches/{id}` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| POST | `/api/store/stock/adjust` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| POST | `/api/store/stock/write-off-expired` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| GET | `/api/store/stock/transactions` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| GET | `/api/store/stock/expiring` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| GET | `/api/store/stock/valuation` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| GET | `/api/store/stock/verify-ledger` | [StockController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/stock/controller/StockController.java) |
| GET | `/api/store/tenant` | [TenantController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/tenant/controller/TenantController.java) |
| PUT | `/api/store/tenant` | [TenantController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/tenant/controller/TenantController.java) |
| POST | `/api/store/users` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |
| GET | `/api/store/users` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |
| GET | `/api/store/users/{id}` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |
| PUT | `/api/store/users/{id}` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |
| PATCH | `/api/store/users/{id}/active` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |
| PATCH | `/api/store/users/{id}/role` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |
| PUT | `/api/store/users/me` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |
| POST | `/api/store/users/me/profile-pic/upload-url` | [StoreUserController](../../Vetalya_backend/src/main/java/com/pet/vetalya/medshop/user/controller/StoreUserController.java) |

## owner

| Method | Path | Controller |
| --- | --- | --- |
| GET | `/api/vets/{vetUserId}/availability` | [AvailabilityController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/AvailabilityController.java) |
| POST | `/api/owner/appointments` | [OwnerAppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerAppointmentController.java) |
| GET | `/api/owner/appointments` | [OwnerAppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerAppointmentController.java) |
| GET | `/api/owner/appointments/{id}` | [OwnerAppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerAppointmentController.java) |
| PUT | `/api/owner/appointments/{id}/reschedule` | [OwnerAppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerAppointmentController.java) |
| PATCH | `/api/owner/appointments/{id}/cancel` | [OwnerAppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerAppointmentController.java) |
| GET | `/api/owner/dashboard` | [OwnerDashboardController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerDashboardController.java) |
| POST | `/api/owner/profile` | [OwnerProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerProfileController.java) |
| GET | `/api/owner/profile` | [OwnerProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerProfileController.java) |
| PUT | `/api/owner/profile` | [OwnerProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerProfileController.java) |
| DELETE | `/api/owner/account` | [OwnerProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerProfileController.java) |
| GET | `/api/owner/reminders` | [OwnerReminderController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerReminderController.java) |
| POST | `/api/owner/appointments/{id}/review` | [OwnerReviewController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerReviewController.java) |
| GET | `/api/owner/reviews` | [OwnerReviewController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerReviewController.java) |
| PUT | `/api/owner/reviews/{reviewId}` | [OwnerReviewController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerReviewController.java) |
| DELETE | `/api/owner/reviews/{reviewId}` | [OwnerReviewController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerReviewController.java) |
| GET | `/api/owner/pets/{petId}/vaccinations` | [OwnerVaccinationController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerVaccinationController.java) |
| GET | `/api/owner/vaccinations/due` | [OwnerVaccinationController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/OwnerVaccinationController.java) |
| POST | `/api/owner/pets` | [PetController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/PetController.java) |
| GET | `/api/owner/pets` | [PetController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/PetController.java) |
| GET | `/api/owner/pets/{petId}` | [PetController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/PetController.java) |
| PUT | `/api/owner/pets/{petId}` | [PetController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/PetController.java) |
| DELETE | `/api/owner/pets/{petId}` | [PetController](../../Vetalya_backend/src/main/java/com/pet/vetalya/owner/controller/PetController.java) |

## vet

| Method | Path | Controller |
| --- | --- | --- |
| GET | `/api/vet/appointments` | [AppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/AppointmentController.java) |
| GET | `/api/vet/appointments/{appointmentId}` | [AppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/AppointmentController.java) |
| PATCH | `/api/vet/appointments/{appointmentId}/status` | [AppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/AppointmentController.java) |
| PUT | `/api/vet/appointments/{appointmentId}/notes` | [AppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/AppointmentController.java) |
| GET | `/api/vet/appointments/dashboard` | [AppointmentController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/AppointmentController.java) |
| GET | `/api/vet/dashboard` | [DashboardController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/DashboardController.java) |
| POST | `/api/vet/hospitals` | [HospitalController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/HospitalController.java) |
| GET | `/api/vet/hospitals` | [HospitalController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/HospitalController.java) |
| GET | `/api/vet/hospitals/{hospitalId}` | [HospitalController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/HospitalController.java) |
| PUT | `/api/vet/hospitals/{hospitalId}` | [HospitalController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/HospitalController.java) |
| DELETE | `/api/vet/hospitals/{hospitalId}` | [HospitalController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/HospitalController.java) |
| GET | `/api/vet/reminders` | [ReminderController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/ReminderController.java) |
| GET | `/api/vet/reviews` | [ReviewController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/ReviewController.java) |
| GET | `/api/vet/reviews/summary` | [ReviewController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/ReviewController.java) |
| POST | `/api/vet/pets/{petId}/vaccinations` | [VaccinationController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VaccinationController.java) |
| GET | `/api/vet/pets/{petId}/vaccinations` | [VaccinationController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VaccinationController.java) |
| PUT | `/api/vet/vaccinations/{vaccinationId}` | [VaccinationController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VaccinationController.java) |
| DELETE | `/api/vet/vaccinations/{vaccinationId}` | [VaccinationController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VaccinationController.java) |
| GET | `/api/vet/vaccinations/due` | [VaccinationController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VaccinationController.java) |
| POST | `/api/vet/profile` | [VetProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VetProfileController.java) |
| GET | `/api/vet/profile` | [VetProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VetProfileController.java) |
| PUT | `/api/vet/profile` | [VetProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VetProfileController.java) |
| DELETE | `/api/vet/account` | [VetProfileController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VetProfileController.java) |
| GET | `/api/vets/search` | [VetSearchController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VetSearchController.java) |
| GET | `/api/vets/{vetUserId}` | [VetSearchController](../../Vetalya_backend/src/main/java/com/pet/vetalya/vet/controller/VetSearchController.java) |
