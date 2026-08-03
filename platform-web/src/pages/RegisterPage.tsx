import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { register, fetchOrganizationName } from '../api/authApi';
import { ApiError } from '../api/client';

export function RegisterPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const joinOrganizationId = searchParams.get('joinOrganization');
  const [joinOrganizationName, setJoinOrganizationName] = useState<string | null>(null);
  const [joinOrganizationError, setJoinOrganizationError] = useState(false);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [createOrganization, setCreateOrganization] = useState(false);
  const [organizationName, setOrganizationName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!joinOrganizationId) return;
    fetchOrganizationName(joinOrganizationId)
      .then((res) => setJoinOrganizationName(res.name))
      .catch(() => setJoinOrganizationError(true));
  }, [joinOrganizationId]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register({
        username,
        email,
        password,
        confirmPassword,
        firstName,
        lastName,
        termsAccepted,
        organizationName: !joinOrganizationId && createOrganization ? organizationName : undefined,
        joinOrganizationId: joinOrganizationId ?? undefined,
      });
      navigate('/login', { replace: true });
    } catch (e) {
      setError(e instanceof ApiError ? e.body?.message ?? t('register.errorFailed') : t('register.errorUnexpected'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <h1>{t('register.title')}</h1>
      {error && <p className="error">{error}</p>}
      <form onSubmit={handleSubmit}>
        <label>
          {t('register.firstName')}
          <input type="text" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
        </label>
        <label>
          {t('register.lastName')}
          <input type="text" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
        </label>
        <label>
          {t('register.username')}
          <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} required />
        </label>
        <label>
          {t('register.email')}
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          {t('register.password')}
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <label>
          {t('register.confirmPassword')}
          <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
        </label>
        {joinOrganizationId ? (
          <p>
            {joinOrganizationError
              ? t('register.joiningOrganizationError')
              : t('register.joiningOrganization', { name: joinOrganizationName ?? '...' })}
          </p>
        ) : (
          <>
            <label>
              <input
                type="checkbox"
                checked={createOrganization}
                onChange={(e) => setCreateOrganization(e.target.checked)}
              />
              {t('register.createOrganization')}
            </label>
            {createOrganization && (
              <label>
                {t('register.organizationName')}
                <input
                  type="text"
                  value={organizationName}
                  onChange={(e) => setOrganizationName(e.target.value)}
                  required
                />
              </label>
            )}
          </>
        )}
        <label>
          <input
            type="checkbox"
            checked={termsAccepted}
            onChange={(e) => setTermsAccepted(e.target.checked)}
          />
          {t('register.termsAccepted')}
        </label>
        <button type="submit" disabled={submitting || joinOrganizationError}>{t('register.submit')}</button>
      </form>
    </div>
  );
}