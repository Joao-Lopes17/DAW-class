import * as React from "react";
import { useParams, useNavigate, Outlet, Link } from "react-router-dom";

type State = {
    channel?: Channel;
    loading: boolean;
    deleting?: boolean;
    error?: string;
    deleteError?: string;
};

type Channel = {
    name: string;
    owner: string;
    type: string;
};

type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success"; payload: Channel }
    | { type: "fetch_error"; error: string }
    | { type: "delete_start" }
    | { type: "delete_success" }
    | { type: "delete_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { channel: action.payload, loading: false, error: undefined };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        case "delete_start":
            return { ...state, deleting: true, deleteError: undefined };
        case "delete_success":
            return { ...state, deleting: false, channel: undefined };
        case "delete_error":
            return { ...state, deleting: false, deleteError: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function ChannelDetails() {
    const { channelId } = useParams();
    const navigate = useNavigate();
    const [state, dispatch] = React.useReducer(reducer, {
        channel: undefined,
        loading: false,
        deleting: false,
    });

    React.useEffect(() => {
        if (!channelId) return;

        dispatch({ type: "fetch_start" });

        fetch(`http://localhost:8080/api/channels/${channelId}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`Error fetching channel details: ${response.statusText}`);
                }
                return response.json();
            })
            .then((data) => {
                dispatch({ type: "fetch_success", payload: data });
            })
            .catch((err) =>
                dispatch({ type: "fetch_error", error: err.message || "Unknown error" })
            );
    }, [channelId]);

    const handleDeleteChannel = () => {
        if (!channelId || !state.channel) return;

        dispatch({ type: "delete_start" });

        fetch(`http://localhost:8080/api/channels/${channelId}`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ ownername: state.channel.owner }),
        })
            .then((response) => {
                if (!response.ok) {
                    return response.text().then((text) => {
                        throw new Error(text || "An error occurred");
                    });
                }
                dispatch({ type: "delete_success" });
                navigate("/channels/public");
            })
            .catch((err) => {
                dispatch({ type: "delete_error", error: err.message });
            });
    };

    if (state.loading) return <p>Loading channel details...</p>;
    if (state.error) return <p>Error: {state.error}</p>;

    if (!state.channel) {
        return <p>No channel data available.</p>;
    }

    return (
        <div>
            <h2>Channel Details</h2>
            <p>
                <strong>Name:</strong> {state.channel.name}
            </p>
            <p>
                <strong>Owner:</strong> {state.channel.owner}
            </p>
            <p>
                <strong>Type:</strong> {state.channel.type}
            </p>

            <div>
                <nav>
                    <ul>
                        <li>
                            <Link to="createInvitation">Create Invite</Link>
                        </li>
                        <li>
                            <Link to="createInvitationLink">Create invite code</Link>
                        </li>
                        <li>
                            <Link to="messages">View Messages</Link>
                        </li>
                    </ul>
                </nav>
            </div>
            <Outlet />
        </div>
    );
}
