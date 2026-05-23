import { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { loadToken } from "../../lib/auth";

export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation();
  const token = loadToken();
  if (!token) {
    return <Navigate to="/signin" replace state={{ from: location.pathname }} />;
  }
  return children;
}

