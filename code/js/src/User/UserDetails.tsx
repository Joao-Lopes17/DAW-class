import * as React from 'react'
import * as ReactDOM from 'react-dom/client'
import { useParams } from 'react-router-dom'
import { Login } from './Login';

type State = {
    username: string;
    channels: string[];
    loading: boolean;
    error?: string;
};


type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success"; payload: { username: string; channels: string[] } }
    | { type: "fetch_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { ...action.payload, loading: false, error: undefined };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function UserDetails() {
    const { userId } = useParams();
    const [state, dispatch] = React.useReducer(reducer, {
        username: "",
        channels: [],
        loading: false,
    });

    React.useEffect(() => {
        if (!userId) return;

        dispatch({ type: "fetch_start" });

        fetch(`http://localhost:8080/api/users/${userId}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
        })
            .then((response) => {
                if (!response.ok) {
                    console.log("failed")
                    throw new Error(`Error fetching user details: ${response.statusText}`);
                }
                console.log("success")
                return response.json();
            })
            .then((data) => {
                dispatch({
                    type: "fetch_success",
                    payload: {
                        username: data.username,
                        channels: data.channels || [],
                    },
                });
            })
            .catch((err) =>
                dispatch({ type: "fetch_error", error: err.message || "Unknown error" })
            );
    }, [userId]);

    if (state.loading) return <p>Loading user details...</p>;
    if (state.error) return <p>Error: {state.error}</p>;
    

    return (
        <div>
            <h2>User Details</h2>
            <p><strong>Username:</strong> {state.username}</p>
            <p><strong>Channels:</strong></p>
            <ul>
                {state.channels.map((channel) => (
                    <li key={channel}>{channel}</li>
                ))}
            </ul>
        </div>
    );
}

//APENAS PARA TESTE
export function userDetailsTest(){
    const root = ReactDOM.createRoot(document.getElementById('container'))
        // Don't do this => State should be IMMUTABLE => Don't call render
        // ONLY for demo
        root.render(<UserDetails></UserDetails>)

}