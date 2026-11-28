import {useEffect, useState} from 'react'
import {useNavigate} from "react-router";
import axios from "axios";

function startPageFor(role: string) {
    switch (role) {
        case 'ADMIN':
            return '/admin'
        case 'MANUFACTURER':
            return '/manufacturer'
        case 'RECYCLER':
            return '/recycler'
        case 'AUTHORITY':
            return '/authority'
        default:
            return '/'
    }
}

export default function LoginPage() {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState<string | null>(null)
    const navigate = useNavigate()
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        axios.get('/api/auth/me')
            .then(r => navigate(startPageFor(r.data.role)))
            .catch(() => {
            })
            .finally(() => setLoading(false))
    }, [navigate])

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault()
        setError(null)

        try {
            const response =
                await axios.post('/api/auth/login', {username, password});
            const currentUser = response.data
            navigate(startPageFor(currentUser.role))
        } catch (error) {
            if (axios.isAxiosError(error)) {
                setError(error.response?.data?.detail ?? 'Anmeldung fehlgeschlagen')
            }
        }
    }

    if (loading) return <p>Lade...</p>


    return (
        <form onSubmit={handleSubmit}>
            <h1>Anmelden</h1>
            {error && <p style={{color: 'red'}}>{error}</p>}
            <label>
                Benutzername
                <input value={username} onChange={e => setUsername(e.target.value)}/>
            </label>
            <label>
                Passwort
                <input type="password" value={password} onChange={e => setPassword(e.target.value)}/>
            </label>
            <button type="submit">Anmelden</button>
        </form>
    )
}