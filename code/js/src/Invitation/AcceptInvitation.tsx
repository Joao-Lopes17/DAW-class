import * as React from "react";
import { Outlet, useParams } from "react-router-dom";
import { AuthContext } from "../User/Auth/AuthProvider";

type State = {
    loading: boolean;
    error?: string;
    successMessage?: string;
};

type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success" }
    | { type: "fetch_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined, successMessage: undefined };
        case "fetch_success":
            return { ...state, loading: false, successMessage: "Invitation accepted successfully!" };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}


export function AcceptInvitation() {
    const { invitationId, chId, permission } = useParams()
    const { username } = React.useContext(AuthContext);
    const [state, dispatch] = React.useReducer(reducer, { loading: false });

    React.useEffect(() => {
        if (!invitationId || !username) {
            dispatch({ type: "fetch_error", error: "Invalid invitation ID or username." });
            return;
        }
            
        dispatch({ type: "fetch_start" });

        fetch(`http://localhost:8080/api/invitations/accept`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify({username, invitationId, chId, permission}),
        })
            .then((response) => { 
                if (!response.ok) { 
                    throw new Error("Failed to accept invitation.");
                }
                return response.json();
            })
            .then(() => {
                dispatch({ type: "fetch_success" });
            })
            .catch((error) => {
                dispatch({ type: "fetch_error", error: error.message });
            });
    }, [invitationId, username]);
    if (state.loading) return <p>Accepting invitation...</p>;
    if (state.error) return <p>Error: {state.error}</p>;
    return (
        <div>
            <h2>Accept Invitation</h2>
            {state.successMessage && <p style={{ color: "green" }}>{state.successMessage}</p>}
        <Outlet />
        </div>
        
    );
}
