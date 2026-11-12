import { request } from "../../../services/api";
import { createOwnerRepository } from "../../../../../packages/owner-domain/src/api.js";
export const ownerApi = createOwnerRepository(
  (path, { body, ...options } = {}) =>
    request(path, {
      ...options,
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    }),
);
