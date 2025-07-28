import * as React from "react";
import { useReducer } from "react";
import { Link } from "react-router-dom";

type State = {
    users: User[];
    loading: boolean;
    error?: string;
};

type User = {
    id: string;
    username: string;
    passwordValidation: string;
}


type Action =
    | { type: "fetch_start" }
    | { type: "fetch_success"; payload: User[] }
    | { type: "fetch_error"; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "fetch_start":
            return { ...state, loading: true, error: undefined };
        case "fetch_success":
            return { users: action.payload, loading: false, error: undefined };
        case "fetch_error":
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error("Unhandled action type");
    }
}

export function Users() {
    const [state, dispatch] = useReducer(reducer, {
        users: [],
        loading: false,
    });

    React.useEffect(() => {
        dispatch({ type: "fetch_start" });

        fetch("http://localhost:8080/api/users", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include"
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Failed to fetch users");
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

    if (state.loading) return <p>Loading users...</p>;
    if (state.error) return <p>Error: {state.error}</p>;

    return (
        <div>
            <h2>Users List</h2>
            <ul>
                {state.users.map((user) => (
                    <li key={user.id}>
                        <Link to={`/users/${user.id}`}>{user.username}</Link>
                    </li>
                ))}
            </ul>
        </div>
    );
}
