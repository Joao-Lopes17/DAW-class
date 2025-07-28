import * as React from 'react'

type AuthContextType = {
    username: string | undefined;
    setUsername: (v: string | undefined) => void
}

export const AuthContext = React.createContext<AuthContextType>({
    username: undefined,
    setUsername: () => { throw Error("Not implemented!") }
})

export function AuthProvider({children} : { children: React.ReactNode}) {
    const [user, setUser] = React.useState(undefined)
    return (
        <AuthContext.Provider value={{username: user, setUsername: setUser}}>
            {children}
        </AuthContext.Provider>
    )
}

/**
import * as React from 'react';

type AuthContextType = {
    username: string | undefined;
    setUsername: (v: string | undefined) => void;
    logout: () => void; // Adicionar uma função de logout
};

export const AuthContext = React.createContext<AuthContextType>({
    username: undefined,
    setUsername: () => { throw Error("Not implemented!"); },
    logout: () => { throw Error("Not implemented!"); },
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = React.useState<string | undefined>(undefined);

    // Verificar cookies ao carregar a aplicação
    React.useEffect(() => {
        fetch("http://localhost:8080/api/verify", {
            credentials: "include", // Inclui os cookies na requisição
        })
            .then((res) => res.json())
            .then((data) => {
                if (data.username) setUser(data.username);
            })
            .catch(() => {
                setUser(undefined); // Caso não esteja autenticado
            });
    }, []);

    // Função de logout
    const logout = () => {
        fetch("http://localhost:8080/api/logout", {
            method: "POST",
            credentials: "include", // Envia os cookies
        }).then(() => setUser(undefined));
    };

    return (
        <AuthContext.Provider value={{ username: user, setUsername: setUser, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

 */