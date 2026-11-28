import {useState} from "react";
import axios from "axios";

export default function LoginPage() {

    const [userName, setUserName] = useState<string>("")
    const [password, setPassword] = useState<string>("")
    const [errorMessage, setErrorMessage] = useState<string>("")


    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage("")
        try {
            const response = await axios.post('/api/auth/login', {
                username: userName,
                password: password
            })
        } catch (error) {
            if (axios.isAxiosError(error)) {
                setErrorMessage(error.response?.data?.detail ?? "Fehler beim Login");
            }
        }
    }

    return (
        <>
            <h1>Login Page</h1>

            {errorMessage &&
                <p style={{color: 'red'}}>{errorMessage}</p>
            }

            <form className={"add-restaurant-form"} onSubmit={handleSubmit}>
                <label>
                    Benutzername
                </label>
                <input value={userName} onChange={event => setUserName(event.target.value)}/>
                <label>
                    Passwort
                </label>
                <input value={password} onChange={event => setPassword(event.target.value)} type="password"/>
                <button>Login</button>
            </form>
        </>
    )
}
