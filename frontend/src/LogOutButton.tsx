import {useNavigate} from "react-router";
import {useState} from "react";
import axios from "axios";

export default function LogOutButton() {
    
    const [error, setError] = useState<string | null>(null)
    const navigate = useNavigate()

    async function handleLogout() {
        try {
            await axios.post('/api/auth/logout')
            navigate('/')
        } catch (error) {
            if (axios.isAxiosError(error)) {
                setError(error.response?.data?.detail ?? 'Abmelden fehlgeschlagen')
            }
        }
    }

    return (
        <>
            {error && <p style={{color: 'red'}}>{error}</p>}
            <button onClick={handleLogout}>Abmelden</button>
        </>
    )
}
