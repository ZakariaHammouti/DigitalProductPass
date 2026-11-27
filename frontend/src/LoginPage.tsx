import {useEffect, useState} from 'react'

type CurrentUser = { username: string; role: string }

export default function LoginPage() {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState<string | null>(null)
    const [user, setUser] = useState<CurrentUser | null>(null)

    useEffect(() => {
        fetch('/api/auth/me')
            .then(response => response.ok ? response.json() : null)
            .then(setUser)
    }, [])

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault()
        setError(null)

        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, password}),
        })

        if (response.ok) {
            setUser(await response.json())
        } else {
            const problem = await response.json()
            setError(problem.detail ?? 'Anmeldung fehlgeschlagen')
        }
    }

    async function handleLogout() {
        await fetch('/api/auth/logout', {method: 'POST'})
        setUser(null)
        setUsername('')
        setPassword('')
    }

    if (user) {
        return (
            <section>
                <p>Angemeldet als {user.username} ({user.role})</p>
                <button onClick={handleLogout}>Abmelden</button>
            </section>
        )
    }

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