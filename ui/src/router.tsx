import { createBrowserRouter, Navigate } from "react-router-dom";
import { AppLayout } from "./views/app/AppLayout";
import { AuditPage } from "./views/app/AuditPage";
import { CaseDetailsPage } from "./views/app/CaseDetailsPage";
import { CasesPage } from "./views/app/CasesPage";
import { SummaryPage } from "./views/app/SummaryPage";
import { TransactionsPage } from "./views/app/TransactionsPage";
import { LandingPage } from "./views/public/LandingPage";
import { SignInPage } from "./views/public/SignInPage";
import { SignUpPage } from "./views/public/SignUpPage";
import { RequireAuth } from "./views/public/auth";

export const router = createBrowserRouter([
  { path: "/", element: <LandingPage /> },
  { path: "/signin", element: <SignInPage /> },
  { path: "/signup", element: <SignUpPage /> },
  {
    path: "/app",
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <SummaryPage /> },
      { path: "transactions", element: <TransactionsPage /> },
      { path: "cases", element: <CasesPage /> },
      { path: "cases/:caseId", element: <CaseDetailsPage /> },
      { path: "audit", element: <AuditPage /> }
    ]
  },
  { path: "*", element: <Navigate to="/" replace /> }
]);

