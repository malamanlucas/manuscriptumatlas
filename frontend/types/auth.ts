// ── Auth / User Management ──

export interface UserDTO {
  id: number;
  email: string;
  displayName: string;
  pictureUrl: string | null;
  role: "ADMIN" | "MEMBER";
}

export interface LoginResponse {
  token: string;
  user: UserDTO;
}
