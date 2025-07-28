import * as React from "react";
import { AuthContext } from "../User/Auth/AuthProvider";
import { useParams } from "react-router-dom";

type State = {
    loading: boolean;
    error?: string;
    successMessage?: string;
};

type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success" }
    | { type: "fetch_error"; error: string }

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { ...state, loading: false, successMessage: "Invitation rejected successfully!" };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function RejectInvitation() {
    const {username} = React.useContext(AuthContext)
    const {invitationId} = useParams()
    const [state, dispatch] = React.useReducer(reducer, {
       
        loading: false,
    });

    React.useEffect(() => {
        if (state.successMessage || state.error) {
            const timer = setTimeout(() => {
                dispatch({ type: "fetch_success" });
            }, 5000);
            return () => clearTimeout(timer);
        }
    }, [state.successMessage, state.error]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        dispatch({ type: "fetch_start" });

        try {
            const response = await fetch("http://localhost:8080/api/invitations/reject", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ username, invitationId }),
            });

            if (!response.ok) {
                throw new Error("Failed to reject invitation.");
            }

            dispatch({ type: "fetch_success" });
        } catch (err) {
            dispatch({ type: "fetch_error", error: err.message });
        }
    };

    return (
        <div>
            <h2>Reject Invitation</h2>
            <form onSubmit={handleSubmit}>
                <button type="submit" disabled={state.loading}>
                    {state.loading ? "Rejecting..." : "Reject Invitation"}
                </button>
            </form>
            {state.error && <p style={{ color: "red" }}>{state.error}</p>}
            {state.successMessage && (
                <p style={{ color: "green" }}>{state.successMessage}</p>
            )}
        </div>
    );
}