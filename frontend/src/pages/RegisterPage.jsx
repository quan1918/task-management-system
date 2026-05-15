import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { register } from "../api";


const EyeIcon = ({ open }) => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        {open ? (
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
        ) : (
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
        )}
    </svg>
);

const inputCls = "mb-4 px-4 w-full h-11 border border-gray-300 rounded-lg text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition";

const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;

const validatePassword = (pwd) => {
    if (!PASSWORD_REGEX.test(pwd)) {
        return "Password must be at least 8 characters and include uppercase, lowercase, number, and special character.";
    }
    return null;
};

function RegisterPage() {
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        fullName: "",
        username: "",
        email: "",
        password: "",
        confirmPassword: "",
    });

    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [passwordTouched, setPasswordTouched] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
        if (name === "password" || name === "confirmPassword") setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (formData.password !== formData.confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        const passwordError = validatePassword(formData.password);
        if (passwordError) {
            setError(passwordError);
            return;
        }

        setLoading(true);
        try {
            const result = await register({
                fullName: formData.fullName,
                username: formData.username,
                email: formData.email,
                password: formData.password,
            });

            if (result.success) {
                navigate('/login');
            } else {
                setError(result.error || 'Registration failed');
            }
        } catch (err) {
            setError('An error occurred. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 px-4 py-8">
            <div className="w-full max-w-md bg-white rounded-xl shadow-md border border-gray-200 p-8">

                {/* Header */}
                <div className="text-center mb-8">
                    <h1 className="text-2xl font-bold text-gray-900">Create Account</h1>
                    <p className="text-sm text-gray-500 mt-1">Enter your details to create an account</p>
                </div>

                {/* Error banner */}
                {error && (
                    <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
                        {error}
                    </div>
                )}

                {/* Form */}
                <form onSubmit={handleSubmit} className="flex flex-col">
                    <input
                        type="text" name="fullName" placeholder="Full Name" 
                        value={formData.fullName} onChange={handleChange}
                        required autoComplete="name"
                        className={inputCls}
                    />

                    <input
                        type="text" name="username" placeholder="Username" 
                        value={formData.username} onChange={handleChange}
                        required autoComplete="username"
                        className={inputCls}
                    />

                    <input
                        type="email" name="email" placeholder="Email" 
                        value={formData.email} onChange={handleChange}
                        required autoComplete="email"
                        className={inputCls}
                    />

                    {/* Password */}
                    <div className="relative w-full">
                        <input
                            type={showPassword ? "text" : "password"}
                            name="password"
                            placeholder="Password"
                            value={formData.password}
                            onChange={handleChange}
                            onBlur={() => setPasswordTouched(true)}
                            required
                            autoComplete="new-password"
                            className={`${inputCls} pr-11`}
                            maxLength={128}
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(p => !p)}
                            className="absolute right-3 top-[18px] -translate-y-1/2 text-gray-400 hover:text-gray-600"
                            aria-label={showPassword ? 'Hide password' : 'Show password'}>
                            <EyeIcon open={showPassword} />
                        </button>
                    </div>

                    {passwordTouched && formData.password && validatePassword(formData.password) && (
                        <p className="text-xs text-red-500 -mt-3 mb-3 px-1">
                            {validatePassword(formData.password)}
                        </p>
                    )}

                    {/* Confirm Password */}
                    <div className="relative w-full">
                        <input
                            type={showConfirmPassword ? "text" : "password"}
                            name="confirmPassword"
                            placeholder="Confirm Password"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            required
                            autoComplete="new-password"
                            className={`${inputCls} pr-11`}
                            maxLength={128}
                        />
                        <button type="button"
                            onClick={() => setShowConfirmPassword(p => !p)}
                            className="absolute right-3 top-[18px] -translate-y-1/2 text-gray-400 hover:text-gray-600"
                            aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}>
                            <EyeIcon open={showConfirmPassword} />
                        </button>
                    </div>
                    {/* Submit*/}
                    <button type="submit" disabled={loading}
                        className="w-full h-11 bg-blue-500 text-white font-semibold rounded-lg text-sm cursor-pointer transition-colors hover:bg-blue-600 active:bg-blue-700 disabled:opacity-60 disabled:cursor-not-allowed mt-2">
                        {loading ? 'Creating account...' : 'Create Account'}
                    </button>
                </form>

                {/* Driver */}
                <div className="border-t border-gray-200 mt-6 pt-5 text-center text-sm text-gray-500">
                    Already have an account?{' '}
                    <Link to="/login" className="text-blue-500 font-semibold hover:underline">
                        Sign In
                    </Link>
                </div>
            </div>
        </div>
    );
}

export default RegisterPage;