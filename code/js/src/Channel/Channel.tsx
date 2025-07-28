import * as React from "react";
import { Link, Outlet } from "react-router-dom";
import { AuthContext } from "../User/Auth/AuthProvider";

type State = {
    channels: Channel[];
    loading: boolean;
    error?: string;
};

type Channel = {
    id: string;
    name: string;
    owner: string;
    type: string;
    users: string[];
};

type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success"; payload: Channel[] }
    | { type: "fetch_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { channels: action.payload, loading: false, error: undefined };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function Channels() {
    const { username } = React.useContext(AuthContext); 
    const [state, dispatch] = React.useReducer(reducer, {
        channels: [],
        loading: false,
    });

    React.useEffect(() => {
        dispatch({ type: "fetch_start" });

        fetch(`http://localhost:8080/api/channels/owner/${username}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("User is not in any channel");
                }
                return response.json();
            })
            .then((data) => {
                dispatch({ type: "fetch_success", payload: data });
            })
            .catch((err) => {
                dispatch({ type: "fetch_error", error: err.message });
            });
    }, []);

    if (state.loading) return <p>Loading channels...</p>;
    if (state.error) return <p>Error: {state.error}</p>;

    return (
        <div>
            <h2>Users Channels List</h2>
            <ul>
                {state.channels.map((channel) => (
                    <li key={channel.id}>
                        <Link to={`/channels/${channel.id}`}>{channel.name}</Link>
                    </li>
                ))}
            </ul>
            <Outlet />
        </div>
    );
}

