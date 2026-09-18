CREATE ROLE permitflow_app
  LOGIN
  PASSWORD 'permitflow_test_password'
  NOSUPERUSER
  NOBYPASSRLS;

GRANT ALL PRIVILEGES ON DATABASE permitflow_test TO permitflow_app;
GRANT USAGE, CREATE ON SCHEMA public TO permitflow_app;