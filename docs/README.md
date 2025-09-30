# TIM 2.0 Documentation Site

This directory contains the GitHub Pages documentation for TIM 2.0.

## Setup for GitHub Pages

### Option 1: Project Pages (Recommended)
This setup allows the documentation to be accessible at `https://buerostack.github.io/TIM/`

1. **Repository Settings:**
   - Go to repository Settings → Pages
   - Source: Deploy from a branch
   - Branch: Select `main` or `wip/tim_2.0`
   - Folder: `/docs`

2. **Multi-Repository Support:**
   - Each repository in the buerostack organization can have its own `/docs` folder
   - They will automatically be available at `https://buerostack.github.io/REPOSITORY_NAME/`
   - No conflicts between different project documentation

### Option 2: Organization Pages (Alternative)
For a centralized documentation site at `https://buerostack.github.io/`

1. Create a repository named `buerostack.github.io`
2. Move documentation files to the root of that repository
3. All projects would be subdirectories under the main site

## Local Development

### Prerequisites
- Ruby 2.7+
- Bundler gem

### Setup
```bash
# Install dependencies
cd docs
bundle install

# Serve locally
bundle exec jekyll serve

# View at http://localhost:4000/TIM/
```

### Content Guidelines

#### Supported Claims Only
- ✅ Feature descriptions based on actual implementation
- ✅ Architecture explanations with code references
- ✅ Deployment instructions that have been tested
- ❌ Performance metrics without benchmarks
- ❌ Comparison claims without evidence
- ❌ Marketing language without technical backing

#### Mermaid Diagrams
- All architecture diagrams are rendered automatically
- Use proper Mermaid syntax in code blocks
- Test diagrams locally before committing

#### Navigation
- Main navigation is configured in `_config.yml`
- Each major section should have an `index.md` file
- Use relative links between documentation pages

## File Structure

```
docs/
├── _config.yml           # Jekyll configuration
├── _layouts/
│   └── default.html      # Custom layout with Mermaid support
├── index.md              # Landing page
├── api/
│   └── index.md          # API documentation
├── architecture/
│   └── index.md          # Technical architecture
├── demo/
│   └── index.md          # Interactive demos
├── deployment/
│   └── index.md          # Deployment guides
├── migration/
│   └── index.md          # KeyCloak migration guide
└── security/
    └── index.md          # Security features
```

## Publishing Checklist

Before enabling GitHub Pages:

- [ ] Remove all unsupported performance claims
- [ ] Verify all links work with the `/TIM/` base path
- [ ] Test Mermaid diagrams render correctly
- [ ] Ensure demo examples use correct localhost URLs
- [ ] Check that all markdown files have proper front matter
- [ ] Validate Jekyll configuration

## Maintenance

### Regular Updates
- Update API examples when endpoints change
- Refresh deployment instructions for new versions
- Keep migration guide current with KeyCloak changes
- Update security documentation for new features

### Content Verification
- All technical claims should be verifiable in the codebase
- Performance comparisons should be based on actual measurements
- Migration examples should be tested with real deployments