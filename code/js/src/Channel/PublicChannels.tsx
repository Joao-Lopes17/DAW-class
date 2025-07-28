import * as React from "react";
import { AuthContext } from "../User/Auth/AuthProvider";


type State = {
    channels: Channel[];
    loading: boolean;
    error?: string;
    joining?: string;
};

type Channel = {
    id: string;
    name: string;
    owner: string; // ou o tipo correto
    type: string;  // ou enum se aplicável
    users: string[]; // ou outra estrutura se necessário
};


type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success"; payload: Channel[]}
    | { type: "fetch_error"; error: string }
    | { type: "join_start"; channel: string }
    | { type: "join_success"; channel: string }
    | { type: "join_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { channels: action.payload, loading: false, error: undefined };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        case "join_start":
            return { ...state, joining: action.channel, error: undefined };
        case "join_success":
            return { ...state, joining: undefined };
        case "join_error":
            return { ...state, joining: undefined, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function PublicChannels() {
    const { username } = React.useContext(AuthContext);
    const [state, dispatch] = React.useReducer(reducer, {
        channels: [],
        loading: false,
    });
    

    React.useEffect(() => {
        dispatch({ type: "fetch_start" });

        fetch("http://localhost:8080/api/channels/public")
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Failed to fetch public channels");
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

    function handleJoin(channelId: string) {
        if (!username) {
            dispatch({ type: "join_error", error: "User not logged in" });
            return;
        }

        const permissions = "READ_WRITE";

        dispatch({ type: "join_start", channel: channelId });

        fetch(`http://localhost:8080/api/channels/addParticipant`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify({ username, channelId, permissions }),
        })
            .then((response) => {
                if (!response.ok) {
                    if (response.status === 409) {
                        throw new Error("User is already a participant in the channel");
                    } else if (response.status === 404) {
                        throw new Error("Channel or user not found");
                    }
                    throw new Error("Failed to join channel");
                }
                dispatch({ type: "join_success", channel: channelId });
            })
            .catch((err) => {
                dispatch({ type: "join_error", error: err.message });
            });
    }

    if (state.loading) return <p>Loading public channels...</p>;
    if (state.error) return <p>Error: {state.error}</p>;

    return (
        <div>
            <h2>Search Public Channels</h2>
            <ul>
                {state.channels.map((channel) => (
                    <li key={channel.id}>
                        <span>{channel.name}</span>
                        <button
                            onClick={() => handleJoin(channel.id)}
                            disabled={state.joining === channel.id}
                        >
                            {state.joining === channel.id ? "Joining..." : "Join"}
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    );
}
