import * as React from "react";
import { Link, Outlet, useParams } from "react-router-dom";

type State = {
    invitation?: Invitation;
    loading: boolean;
    error?: string;
};

type Invitation = {
    id: string;
    code: string;
    inviter: User;
    invitee?: User;
    channelid: number;
    used: boolean;
    type: string;
};

type User = {
    id: number;
    username: string;
    passwordValidation: string;
};

type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success"; payload: Invitation }
    | { type: "fetch_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { invitation: action.payload, loading: false, error: undefined };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function InvitationDetails() {
    const { invitationId } = useParams(); // ObtÃ©m o ID do convite da URL
    const [state, dispatch] = React.useReducer(reducer, {
        loading: true,
    });

    React.useEffect(() => {
        dispatch({ type: "fetch_start" });

        fetch(`http://localhost:8080/api/invitations/${invitationId}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Failed to fetch invitation details.");
                }
                return response.json();
            })
            .then((data) => dispatch({ type: "fetch_success", payload: data }))
            .catch((err) => dispatch({ type: "fetch_error", error: err.message }));
    }, [invitationId]);

    if (state.loading) return <p>Loading invitation details...</p>;
    if (state.error) return <p>Error: {state.error}</p>;
    const chId = state.invitation?.channelid
    const permission = state.invitation?.type

    return (
        <div>
            <h2>Invitation Details</h2>
            <p>Invitation Code: {state.invitation?.code}</p>
            <p>Inviter: {state.invitation?.inviter.username}</p>
            <p>Used: {state.invitation?.used ? "Yes" : "No"}</p>
            <p>Permission: {state.invitation?.type}</p>
        <div>
         <Link to={`/invitations/${state.invitation?.id}/accept/${chId}/${permission}`}> Accept Invitation </Link>
         <Link to={`/invitations/${state.invitation?.id}/reject`}> Reject Invitation </Link>
        </div>
        <Outlet />
        </div>
         

    );
}