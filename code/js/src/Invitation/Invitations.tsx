import * as React from "react";
import { Link, Outlet } from "react-router-dom";
import { AuthContext } from "../User/Auth/AuthProvider";

type State = {
    invitations: Invitation[];
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
    id: number,
    username: string,
    passwordValidation: string,
}


type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success"; payload: Invitation[] }
    | { type: "fetch_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { invitations: action.payload, loading: false, error: undefined };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function Invitations() {
    const { username } = React.useContext(AuthContext)
    const [state, dispatch] = React.useReducer(reducer, {
        invitations: [],
        loading: false,
    });

    React.useEffect(() => {
        dispatch({ type: "fetch_start" });

        fetch(`http://localhost:8080/api/invitations/byUser/${username}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Failed to fetch invitations.");
                }
                return response.json();
            })
            .then((data) => dispatch({ type: "fetch_success", payload: data }))
            .catch((err) => dispatch({ type: "fetch_error", error: err.message }));
    }, []);

    if (state.loading) return <p>Loading invitations...</p>;
    if (state.error) return <p>Error: {state.error}</p>;

    return (
        <div>
            <h2>Invitations</h2>
            <ul>
                {state.invitations.map((invitation) => (
                    <li key={invitation.id}>
                        <Link to={`/invitations/${invitation.id}`}>View invitation</Link>
                    </li>
                ))}
            </ul>
            <Outlet />
        </div>
    );
}