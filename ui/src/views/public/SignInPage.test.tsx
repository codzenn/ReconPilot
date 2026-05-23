import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { SignInPage } from "./SignInPage";
import { beforeEach, describe, expect, it } from "vitest";

describe("SignInPage", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("enables submit after input", async () => {
    render(
      <MemoryRouter>
        <SignInPage />
      </MemoryRouter>
    );

    const button = screen.getByRole("button", { name: /sign in/i });
    expect(button).toBeDisabled();

    await userEvent.type(screen.getByLabelText(/email/i), "user@example.com");
    await userEvent.type(screen.getByLabelText(/password/i), "Password1!");

    expect(button).toBeEnabled();
  });
});

