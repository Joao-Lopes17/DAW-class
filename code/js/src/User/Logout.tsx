import * as React from "react";
import { AuthContext } from "./Auth/AuthProvider";
import { Navigate } from "react-router-dom";

export function Logout() {
    const { setUsername } = React.useContext(AuthContext);
    const [isComplete, setIsComplete] = React.useState(false);
    const [error, setError] = React.useState<string | undefined>();

    React.useEffect(() => {
        let isCancelled = false;

        setUsername(undefined);

        // Connection with backend
        fetch("http://localhost:8080/api/logout", { method: "POST"})
            .then((response) => {
                if (!isCancelled) {
                    if (response.ok) {
                        setIsComplete(true);
                    } else {
                        return response.text().then((text) => {
                            throw new Error(text || "Logout failed");
                        });
                    }
                }
            })
            .catch((err) => {
                if (!isCancelled) {
                    console.error("Error during logout:", err);
                    setError(err.message);
                }
            });
        return () => {
            isCancelled = true;
        };
    }, [setUsername]);

    if (error) {
        return <p>Error during logout: {error}</p>;
    }

    if (isComplete) {
        return <Navigate to="/api/users/login" replace />;
    }

    return <p>Logging out...</p>;
}
