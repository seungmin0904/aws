import React, { useRef } from "react";
import "@/styles/ParticleButton.css";

const ParticleButton = ({ children, type = "circle", className = "", onClick }) => {
  const buttonRef = useRef(null);

  const handleClick = (e) => {
    const rect = buttonRef.current.getBoundingClientRect();
    const x = e.clientX;
    const y = e.clientY;

    for (let i = 0; i < 30; i++) {
      createParticle(x, y, type);
    }

    if (onClick) onClick(e);
  };

  const createParticle = (x, y, type) => {
    const particle = document.createElement("particle");
    document.body.appendChild(particle);

    const size = Math.floor(Math.random() * 20 + 5);
    particle.style.width = `${size}px`;
    particle.style.height = `${size}px`;

    const destinationX = x + (Math.random() - 0.5) * 2 * 75;
    const destinationY = y + (Math.random() - 0.5) * 2 * 75;

    switch (type) {
      case "square":
        particle.style.background = `hsl(${Math.random() * 90 + 270}, 70%, 60%)`;
        particle.style.border = "1px solid white";
        break;
      case "circle":
        particle.style.background = `hsl(${Math.random() * 90 + 180}, 70%, 60%)`;
        particle.style.borderRadius = "50%";
        break;
      default:
        particle.style.background = `hsl(${Math.random() * 90 + 180}, 70%, 60%)`;
    }

    const animation = particle.animate(
      [
        {
          transform: `translate(${x - size / 2}px, ${y - size / 2}px)`,
          opacity: 1,
        },
        {
          transform: `translate(${destinationX}px, ${destinationY}px)`,
          opacity: 0,
        },
      ],
      {
        duration: 500 + Math.random() * 1000,
        easing: "cubic-bezier(0, .9, .57, 1)",
        delay: Math.random() * 200,
      }
    );

    animation.onfinish = () => {
      particle.remove();
    };
  };

  return (
    <button
      ref={buttonRef}
      className={`button ${className}`}
      data-type={type}
      onClick={handleClick}
    >
      {children}
    </button>
  );
};

export default ParticleButton;